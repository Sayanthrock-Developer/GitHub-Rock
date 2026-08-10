import subprocess
try:
    subprocess.run(["git", "push", "-f", "origin", "HEAD:jules-736564939005166653-50091147"], check=True)
except Exception as e:
    print(e)
