import os
import cv2
import pathlib
import requests
from datetime import datetime

class ChangeDetection:
    result_prev = []
    HOST = 'http://127.0.0.1:8000'
    username = 'jimin'
    password = 'Q@12121212'
    token = ''
    title = ''
    text = ''
    detection_count = {}
    
    # 필터링할 객체 목록 (원하는 객체만 추가!)
    FILTER_OBJECTS = ['microwave']
    
    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]
        
        # JSON 형식으로 토큰 요청
        res = requests.post(
            self.HOST + '/api-token-auth/',
            json={
                'username': self.username,
                'password': self.password,
            }
        )
        res.raise_for_status()
        self.token = res.json()['token']
        print(f"Token obtained: {self.token}")
        print(f"🔍 Filtering objects: {self.FILTER_OBJECTS}")


    def add(self, names, detected_current, save_dir, image):
        self.title = ''
        self.text = ''
        change_flag = 0
        i = 0
        while i < len(self.result_prev):
            if self.result_prev[i]==0 and detected_current[i]==1:
                # 필터링 조건
                if names[i] in self.FILTER_OBJECTS:
                    change_flag = 1
                    self.title = names[i]
                    self.text += names[i] + ", "
                else:
                    print(f"⏭️  Skipped: {names[i]} (not in filter)")
            i += 1
        
        self.result_prev = detected_current[:]
        
        if change_flag==1:
            # 통계 카운트
            if self.title not in self.detection_count:
                self.detection_count[self.title] = 0
            self.detection_count[self.title] += 1
            
            # 통계 출력
            print("\n" + "="*50)
            print("📊 Detection Statistics:")
            print("="*50)
            for obj, count in sorted(self.detection_count.items()):
                print(f"   {obj}: {count}회")
            print("="*50 + "\n")
            
            self.send(save_dir, image)


    def send(self, save_dir, image):
        now = datetime.now()
        
        today = datetime.now()
        save_path = pathlib.Path(os.getcwd()) / save_dir / 'detected' / str(today.year) / str(today.month) / str(today.day)
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)
        full_path = save_path / '{0}-{1}-{2}-{3}.jpg'.format(today.hour, today.minute, today.second, today.microsecond)
        
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)
        
        # Token 인증 헤더
        headers = {
            'Authorization': 'Token ' + self.token
        }
        
        # Post 데이터
        data = {
            'title': self.title,
            'text': self.text,
            'author': '1'
        }
        
        # 이미지 파일
        files = {
            'image': open(str(full_path), 'rb')
        }
        
        try:
            res = requests.post(
                self.HOST + '/api_root/Post/',
                data=data,
                files=files,
                headers=headers
            )
            print(f"Post response: {res.status_code}")
            if res.status_code == 201:
                print(f"✅ Successfully posted: {self.title}")
            else:
                print(f"❌ Error: {res.text}")
        except Exception as e:
            print(f"❌ Exception: {e}")
        finally:
            files['image'].close()