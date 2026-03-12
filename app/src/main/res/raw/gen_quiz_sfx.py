import os
import wave
import math
import struct

RAW_DIR = os.path.join("app", "src", "main", "res", "raw")
os.makedirs(RAW_DIR, exist_ok=True)


def write_sine(path: str, freq: float, duration: float, sr: int = 44100, amp: float = 0.25):
    n = int(sr * duration)
    fade_in = int(sr * 0.02)
    fade_out = int(sr * 0.04)

    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)  # 16-bit PCM
        w.setframerate(sr)

        for i in range(n):
            t = i / sr
            s = math.sin(2 * math.pi * freq * t)
            g = 1.0
            if i < fade_in:
                g = i / fade_in
            elif i > n - fade_out:
                g = (n - i) / fade_out
                if g < 0:
                    g = 0

            sample = int(max(-1.0, min(1.0, s * amp * g)) * 32767)
            w.writeframes(struct.pack("<h", sample))


write_sine(os.path.join(RAW_DIR, "quiz_correct.wav"), 880, 0.20)
write_sine(os.path.join(RAW_DIR, "quiz_wrong.wav"), 220, 0.28)

for name in ("quiz_correct.wav", "quiz_wrong.wav"):
    p = os.path.join(RAW_DIR, name)
    print(name, os.path.getsize(p))
