import urllib.request
import urllib.parse
import json

def submit():
    req = urllib.request.Request(
        "http://localhost:3000/v1/tools/frontend_verification_complete",
        data=json.dumps({
            "screenshot_path": "/home/jules/verification/screenshots/verification.png",
            "additional_media_paths": ["/home/jules/verification/videos/e13abca32831322e3f93795121b27875.webm"]
        }).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req) as response:
            print(response.read().decode())
    except Exception as e:
        print(e)

submit()
