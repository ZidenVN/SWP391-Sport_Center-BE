import sys

file_path = "src/main/java/com/fptu/swp391/sportscentermanager/service/impl/SportClassServiceImpl.java"
with open(file_path, "r") as f:
    content = f.read()

# Make Coach optional in createClass
content = content.replace(
    "Coach coach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));",
    "// TODO: Xử lý logic gán Coach sau khi hoàn thiện module Coach\n       Coach coach = null;\n       if (requestDTO.getCoachId() != null) {\n           coach = coachRepository.findById(requestDTO.getCoachId()).orElseThrow(() -> new AppException(ErrorCode.COACH_NOT_FOUND));\n       }"
)

# Handle NullPointerException for coachName in createClass
content = content.replace(
    ".coachName(savedClass.getCoach().getFirstName() + \" \" + savedClass.getCoach().getLastName())",
    ".coachName(savedClass.getCoach() != null ? savedClass.getCoach().getFirstName() + \" \" + savedClass.getCoach().getLastName() : \"Chưa có HLV\")"
)

# Handle NullPointerException for coachName in getAllClasses
content = content.replace(
    ".coachName(c.getCoach().getFirstName() + \" \" + c.getCoach().getLastName())",
    ".coachName(c.getCoach() != null ? c.getCoach().getFirstName() + \" \" + c.getCoach().getLastName() : \"Chưa có HLV\")"
)

with open(file_path, "w") as f:
    f.write(content)

