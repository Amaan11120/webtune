from flask import Flask, request, jsonify, send_from_directory
import yt_dlp, os
app = Flask(__name__, static_folder="static")
OUT = os.path.expanduser("~/storage/downloads/WebTune")
os.makedirs(OUT, exist_ok=True)

@app.route("/")
def home(): return send_from_directory("static", "index.html")

@app.route("/dl", methods=["POST"])
def dl():
    d = request.json
    o = {"outtmpl": OUT + "/%(title).80s.%(ext)s", "quiet": True}
    if d["mode"] == "audio":
        o.update(format="bestaudio/best", postprocessors=[{
            "key": "FFmpegExtractAudio", "preferredcodec": "mp3",
            "preferredquality": "192"}])
    else:
        o.update(format="best[height<=720]/best")
    try:
        with yt_dlp.YoutubeDL(o) as y:
            i = y.extract_info(d["url"], download=True)
        return jsonify(ok=True, title=i.get("title"))
    except Exception as e:
        return jsonify(ok=False, error=str(e)[:150])

app.run(port=8080)
