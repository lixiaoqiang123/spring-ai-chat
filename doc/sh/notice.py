import pyttsx3

def say_all_done():
    # 初始化语音引擎
    engine = pyttsx3.init()
    
    # 设置语速（可选，范围通常在 0-200，默认通常是 200）
    engine.setProperty('rate', 150)
    
    # 设置音量（可选，范围 0.0 到 1.0）
    engine.setProperty('volume', 1.0)
    
    # 让电脑说话
    engine.say("All Done")
    
    # 运行并等待播放结束
    engine.runAndWait()

if __name__ == "__main__":
    say_all_done()