#!/usr/bin/env python3
"""Seed dữ liệu demo qua API của BE (không ghi thẳng vào DB, nên dữ liệu đi qua đúng validation/nghiệp vụ).

Chỉ THÊM dữ liệu, không sửa/xóa dữ liệu có sẵn. Chạy lại an toàn: thứ đã có (theo tên/email/lịch) thì bỏ qua.
Mọi tài khoản seed dùng email đuôi @seed.sportcenter.demo để dễ nhận ra / dọn sau này.

Biến môi trường:
  SEED_MANAGER_EMAIL, SEED_MANAGER_PASSWORD  tài khoản Quản lý trung tâm (bắt buộc)
  SEED_PASSWORD                              mật khẩu chung cho mọi tài khoản seed (bắt buộc, >= 6 ký tự)
  SEED_BASE_URL                              mặc định http://localhost:8080/api/v1

Chạy:  python3 scripts/seed_demo.py
"""
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get("SEED_BASE_URL", "http://localhost:8080/api/v1").rstrip("/")
DOMAIN = "seed.sportcenter.demo"
# BE bắt buộc ảnh khi tạo lớp: dùng ảnh trang chủ của repo FE (đặt cạnh repo BE)
IMAGE_DIR = os.environ.get("SEED_IMAGE_DIR", os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                           "..", "..", "SWP391-Sport_Center-FE", "public", "landing"))
SUBJECT_IMAGE = {"Yoga": "yoga.jpg", "Bơi Lội": "swim.jpg", "Gym": "gym.jpg", "Boxing": "boxing.jpg"}


class ApiError(Exception):
    def __init__(self, status, body):
        super().__init__(f"HTTP {status}: {body[:300]}")
        self.status, self.body = status, body
        try:
            self.code = json.loads(body).get("error")
        except Exception:
            self.code = None


def call(method, path, token=None, body=None, raw=None, content_type="application/json"):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", content_type)
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            text = r.read().decode()
            try:
                return json.loads(text) if text else None
            except json.JSONDecodeError:
                return text  # vài API (vd. /auth/register) trả chuỗi thường
    except urllib.error.HTTPError as e:
        raise ApiError(e.code, e.read().decode(errors="replace")) from None


def items(res):
    if isinstance(res, list):
        return res
    return (res or {}).get("content") or (res or {}).get("data") or []


def login(email, password):
    try:
        return call("POST", "/auth/login", body={"email": email, "password": password})["token"]
    except ApiError:
        return None


def log(msg):
    print(msg, flush=True)


# ---------------------------------------------------------------- dữ liệu mẫu

ROOMS = [("Phòng Yoga A", 15), ("Hồ bơi 25m", 20), ("Sàn Boxing", 16), ("Phòng Gym Tầng 2", 20)]

# (tên gói, loại, môn, số ngày, giá, mô tả)
PACKAGES = [
    ("GÓI 3 THÁNG TIẾT KIỆM", "MEMBERSHIP", None, 90, 999000, "Vào trung tâm 90 ngày, dùng mọi khu tập tự do."),
    ("Yoga Cân Bằng 30 ngày", "CLASS", "Yoga", 30, 499000, "Lớp Yoga nhóm 3 buổi/tuần cùng HLV."),
    ("Bơi Cơ Bản 30 ngày", "CLASS", "Bơi Lội", 30, 699000, "Học bơi ếch/sải từ đầu, 2–3 buổi/tuần."),
    ("Gym Kèm HLV 30 ngày", "CLASS", "Gym", 30, 549000, "Tập tạ theo nhóm nhỏ, HLV chỉnh kỹ thuật."),
    ("Fight Club Basic", "CLASS", "Boxing", 30, 799999, None),  # đã có sẵn trên DB: chỉ dùng lại nếu có
]

# (họ, tên, giới tính, môn dạy, slug email). firstName = Họ, lastName = Tên theo quy ước form FE
COACHES = [
    ("Nguyễn", "Minh Tuấn", "MALE", "Gym", "coach.tuan"),
    ("Trần", "Thu Hà", "FEMALE", "Yoga", "coach.ha"),
    ("Lê", "Quốc Bảo", "MALE", "Boxing", "coach.bao"),
    ("Phạm", "Ngọc Lan", "FEMALE", "Bơi Lội", "coach.lan"),
]
RECEPTIONIST = ("Võ", "Thị Mai", "FEMALE", "reception.mai")

MEMBERS = [
    ("Đặng", "Hoàng Nam", "MALE"), ("Bùi", "Thanh Trúc", "FEMALE"), ("Hoàng", "Gia Huy", "MALE"),
    ("Ngô", "Phương Linh", "FEMALE"), ("Đỗ", "Đức Anh", "MALE"), ("Huỳnh", "Mỹ Duyên", "FEMALE"),
    ("Phan", "Tuấn Kiệt", "MALE"), ("Vũ", "Khánh Vy", "FEMALE"), ("Trương", "Minh Khoa", "MALE"),
    ("Lý", "Bảo Ngọc", "FEMALE"), ("Mai", "Anh Thư", "FEMALE"), ("Châu", "Thành Đạt", "MALE"),
]

# (môn, thứ, giờ bắt đầu, giờ kết thúc, phòng, slug HLV, sĩ số)
CLASSES = [
    ("Yoga", "MON,WED,FRI", "06:00", "07:00", "Phòng Yoga A", "coach.ha", 15),
    ("Yoga", "TUE,THU", "18:30", "19:30", "Phòng Yoga A", "coach.ha", 15),
    ("Gym", "MON,WED,FRI", "17:30", "18:30", "Phòng Gym Tầng 2", "coach.tuan", 12),
    ("Boxing", "TUE,THU,SAT", "19:00", "20:30", "Sàn Boxing", "coach.bao", 12),
    ("Bơi Lội", "SAT,SUN", "07:00", "08:00", "Hồ bơi 25m", "coach.lan", 15),
    ("Bơi Lội", "TUE,THU", "17:00", "18:00", "Hồ bơi 25m", "coach.lan", 15),
]

# member thứ i (0-based): mua membership nào, có check-in không, mua gói lớp môn nào (None = không)
#   2 member cuối chỉ tạo giao dịch tại quầy CHỜ THU để lễ tân có việc demo
PLAN = [
    ("GÓI 1 THÁNG LINH HOẠT", True, "Yoga"), ("GÓI 3 THÁNG TIẾT KIỆM", True, "Yoga"),
    ("GÓI 6 THÁNG TOÀN HỆ THỐNG", True, "Gym"), ("GÓI 1 THÁNG LINH HOẠT", True, "Boxing"),
    ("GÓI 3 THÁNG TIẾT KIỆM", True, "Bơi Lội"), ("GÓI 1 THÁNG LINH HOẠT", True, "Gym"),
    ("GÓI 3 THÁNG TIẾT KIỆM", True, "Boxing"), ("GÓI 6 THÁNG TOÀN HỆ THỐNG", True, "Bơi Lội"),
    ("GÓI 1 THÁNG LINH HOẠT", True, None), ("GÓI 3 THÁNG TIẾT KIỆM", False, None),
    ("GÓI 1 THÁNG LINH HOẠT", "PENDING", None), ("GÓI 3 THÁNG TIẾT KIỆM", "PENDING", None),
]
METHODS = ["CASH", "CARD", "BANK_TRANSFER", "E_WALLET"]


def phone(prefix, i):
    return f"09{prefix}{i:06d}"  # 10 số, bắt đầu 09: 09 + 2 số nhóm + 6 số thứ tự


def next_monday(today):
    return today + dt.timedelta(days=(7 - today.weekday()) % 7 or 7)


# ---------------------------------------------------------------- các bước

def seed_rooms(mgr):
    existing = {r["roomName"]: r for r in items(call("GET", "/rooms", mgr))}
    for name, cap in ROOMS:
        if name not in existing:
            existing[name] = call("POST", "/rooms", mgr, {"roomName": name, "capacity": cap, "status": "AVAILABLE"})
            log(f"  + phòng {name}")
    return {n: r["roomId"] for n, r in existing.items()}


def seed_packages(mgr, subjects):
    existing = {p["packageName"]: p for p in items(call("GET", "/packages", mgr))}
    for name, ptype, subject, days, price, desc in PACKAGES:
        if name in existing:
            continue
        if desc is None:  # gói chỉ dùng lại, không tạo
            continue
        body = {"packageName": name, "packageType": ptype, "durationDays": days, "price": price,
                "description": desc, "status": "ACTIVE", "subjectId": subjects[subject] if subject else None}
        existing[name] = call("POST", "/packages", mgr, body)
        log(f"  + gói {name}")
    return existing


def seed_coaches(mgr, subjects, password):
    existing = {c["email"].lower(): c for c in items(call("GET", "/coaches", mgr))}
    ids = {}
    for i, (ho, ten, gender, subject, slug) in enumerate(COACHES):
        email = f"{slug}@{DOMAIN}"
        if email not in existing:
            existing[email] = call("POST", "/coaches", mgr, {
                "firstName": ho, "lastName": ten, "gender": gender, "phone": phone(81, i + 1),
                "email": email, "password": password, "subjectIds": [subjects[subject]]})
            log(f"  + HLV {ho} {ten} ({subject})")
        ids[slug] = existing[email]["coachId"]
    return ids


def seed_receptionist(mgr, password):
    ho, ten, gender, slug = RECEPTIONIST
    email = f"{slug}@{DOMAIN}"
    token = login(email, password)
    if not token:
        call("POST", "/manager/receptionists", mgr, {"firstName": ho, "lastName": ten, "gender": gender,
                                                     "email": email, "phone": phone(82, 1), "password": password})
        log(f"  + lễ tân {ho} {ten}")
        token = login(email, password)
    if not token:
        sys.exit(f"Không đăng nhập được lễ tân {email}")
    return token


def seed_classes(mgr, subjects, rooms, coaches):
    today = dt.date.today()
    start = next_monday(today)
    existing = items(call("GET", "/classes?size=200", mgr))
    taken = {(c.get("subjectName"), c.get("daysOfWeek"), str(c.get("startTime", ""))[11:16])
             for c in existing if c.get("status") not in ("CANCELLED", "STOPPED")}
    result = {}
    for subject, days, t1, t2, room, coach, cap in CLASSES:
        key = (subject, days, t1)
        if key in taken:
            continue
        data = {"subjectId": subjects[subject], "roomId": rooms[room], "coachId": coaches[coach],
                "maxCapacity": cap, "startDate": start.isoformat(), "durationDays": 30,
                "daysOfWeek": days, "startClock": t1, "endClock": t2}
        image_name = SUBJECT_IMAGE.get(subject, "gym.jpg")
        with open(os.path.join(IMAGE_DIR, image_name), "rb") as f:
            image = f.read()
        boundary = uuid.uuid4().hex
        raw = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\n"
               f"Content-Type: application/json\r\n\r\n{json.dumps(data)}\r\n"
               f"--{boundary}\r\nContent-Disposition: form-data; name=\"image\"; filename=\"{image_name}\"\r\n"
               f"Content-Type: image/jpeg\r\n\r\n").encode() + image + f"\r\n--{boundary}--\r\n".encode()
        try:
            c = call("POST", "/classes", mgr, raw=raw, content_type=f"multipart/form-data; boundary={boundary}")
            log(f"  + lớp {subject} {days} {t1}-{t2} từ {start}")
            result[key] = c["classId"]
        except ApiError as e:
            log(f"  ! bỏ qua lớp {subject} {days} {t1}: {e.code or e}")
    return result


def member_email(i):
    return f"member{i + 1:02d}@{DOMAIN}"


def seed_members(password):
    out = []
    for i, (ho, ten, gender) in enumerate(MEMBERS):
        email = member_email(i)
        token = login(email, password)
        if not token:
            call("POST", "/auth/register", body={"firstName": ho, "lastName": ten, "gender": gender,
                                                 "phone": phone(83, i + 1), "email": email,
                                                 "password": password, "confirmPassword": password})
            log(f"  + hội viên {ho} {ten}")
            token = login(email, password)
        me = call("GET", "/members/me", token)
        out.append({"email": email, "token": token, "memberId": me["memberId"], "name": f"{ho} {ten}"})
    return out


def buy(rec, member_id, package_id, method, confirm=True):
    sub = call("POST", f"/receptionist/members/{member_id}/subscriptions", rec,
               {"packageId": package_id, "paymentMethod": method})
    if confirm:
        call("PATCH", f"/receptionist/payments/{sub['paymentId']}/confirm", rec,
             {"note": "Seed demo", "transactionRef": f"SEED-{sub['paymentId']}" if method == "BANK_TRANSFER" else None})
    return sub


def seed_purchases(rec, members):
    # đọc lại để có đủ thông tin môn của cả gói vừa tạo
    packages = {p["packageName"]: p for p in items(call("GET", "/packages", rec))}
    open_classes = {}
    for c in items(call("GET", "/classes?size=200", rec)):
        if c.get("status") == "OPENING" and c.get("durationDays") == 30:
            open_classes.setdefault(c["subjectName"], []).append(c["classId"])
    class_pkg = {p["subject"]["subjectName"]: p["packageId"] for p in packages.values()
                 if p.get("packageType") == "CLASS" and p.get("subject") and p.get("durationDays") == 30
                 and p.get("status") == "ACTIVE"}
    for i, (m, (mship, checkin, subject)) in enumerate(zip(members, PLAN)):
        if items(call("GET", "/members/me/subscriptions", m["token"])):
            continue  # member này đã có gói từ lần chạy trước
        method = METHODS[i % len(METHODS)]
        pending = checkin == "PENDING"
        buy(rec, m["memberId"], packages[mship]["packageId"], "CASH" if pending else method, confirm=not pending)
        if pending:
            log(f"  ~ {m['name']}: {mship} (chờ thu tại quầy)")
            continue
        if checkin:
            call("POST", f"/receptionist/members/{m['memberId']}/check-ins", rec)
        log(f"  + {m['name']}: {mship}{' + check-in' if checkin else ''}")
        if not subject or subject not in class_pkg:
            continue
        try:
            sub = buy(rec, m["memberId"], class_pkg[subject], method)
            classes = open_classes.get(subject, [])
            for cid in classes[i % max(len(classes), 1):] + classes[:i % max(len(classes), 1)]:
                try:
                    call("POST", f"/receptionist/members/{m['memberId']}/class-registrations", rec,
                         {"classId": cid, "subscriptionId": sub["subscriptionId"]})
                    log(f"    + gói lớp {subject}, xếp vào lớp #{cid}")
                    break
                except ApiError as e:
                    log(f"    ! lớp #{cid}: {e.code or e}")
        except ApiError as e:
            log(f"    ! gói lớp {subject}: {e.code or e}")


def main():
    mgr_email, mgr_pw = os.environ.get("SEED_MANAGER_EMAIL"), os.environ.get("SEED_MANAGER_PASSWORD")
    password = os.environ.get("SEED_PASSWORD", "")
    if not (mgr_email and mgr_pw) or len(password) < 6:
        sys.exit("Thiếu SEED_MANAGER_EMAIL / SEED_MANAGER_PASSWORD hoặc SEED_PASSWORD (>= 6 ký tự). Xem đầu file.")
    try:
        mgr = call("POST", "/auth/login", body={"email": mgr_email.strip(), "password": mgr_pw})["token"]
    except ApiError as e:
        sys.exit(f"Đăng nhập Quản lý thất bại ({mgr_email.strip()}): {e}")
    except urllib.error.URLError as e:
        sys.exit(f"Không kết nối được BE tại {BASE}: {e.reason}")

    subjects = {s["subjectName"]: s["subjectId"] for s in items(call("GET", "/subjects", mgr))
                if s.get("status") == "ACTIVE"}
    missing = {c[3] for c in COACHES} - subjects.keys()
    if missing:
        sys.exit(f"Thiếu bộ môn trên DB: {', '.join(sorted(missing))}")

    log("1. Phòng tập"); rooms = seed_rooms(mgr)
    log("2. Gói tập"); seed_packages(mgr, subjects)
    log("3. Huấn luyện viên"); coaches = seed_coaches(mgr, subjects, password)
    log("4. Lễ tân"); rec = seed_receptionist(mgr, password)
    log("5. Lớp học"); seed_classes(mgr, subjects, rooms, coaches)
    log("6. Hội viên"); members = seed_members(password)
    log("7. Gói, thanh toán, check-in, đăng ký lớp"); seed_purchases(rec, members)
    log(f"Xong. Tài khoản seed: *@{DOMAIN} (member01..{len(MEMBERS):02d}, coach.*, reception.mai)")


if __name__ == "__main__":
    try:
        main()
    except ApiError as e:
        sys.exit(f"Lỗi API: {e}")
