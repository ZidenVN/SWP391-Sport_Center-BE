import sys

# 1. Update SportClassService interface
service_path = "src/main/java/com/fptu/swp391/sportscentermanager/service/SportClassService.java"
with open(service_path, "r") as f:
    content = f.read()

content = content.replace(
    "SportClass getClassById(Long id);",
    "SportClass getClassById(Long id);\n    SportClassResponseDTO getSportClassDTOById(Long id);"
)
with open(service_path, "w") as f:
    f.write(content)

# 2. Update SportClassServiceImpl
impl_path = "src/main/java/com/fptu/swp391/sportscentermanager/service/impl/SportClassServiceImpl.java"
with open(impl_path, "r") as f:
    content = f.read()

dto_method = """
    @Override
    public SportClassResponseDTO getSportClassDTOById(Long id) {
        SportClass c = getClassById(id);
        return SportClassResponseDTO.builder()
            .classId(c.getClassId())
            .maxCapacity(c.getMaxCapacity())
            .scheduleTime(c.getScheduleTime())
            .status(c.getStatus())
            .roomName(c.getRoom().getRoomName())
            .subjectName(c.getSubject().getSubjectName())
            .coachName(c.getCoach() != null ? c.getCoach().getFirstName() + " " + c.getCoach().getLastName() : "Chưa có HLV")
            .build();
    }
"""

content = content.replace(
    "public SportClass getClassById(Long id) {\n        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));\n    }",
    "public SportClass getClassById(Long id) {\n        return sportClassRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));\n    }\n" + dto_method
)

with open(impl_path, "w") as f:
    f.write(content)

# 3. Update SportClassController
controller_path = "src/main/java/com/fptu/swp391/sportscentermanager/controller/SportClassController.java"
with open(controller_path, "r") as f:
    content = f.read()

controller_method = """
    @PreAuthorize("hasAuthority('VIEW_CLASS')")
    @GetMapping("/{id}")
    public ResponseEntity<SportClassResponseDTO> getClassById(@PathVariable Long id){
        return ResponseEntity.ok(sportClassService.getSportClassDTOById(id));
    }
"""

content = content.replace(
    "public ResponseEntity<List<SportClassResponseDTO>> getAllClasses(){\n        return ResponseEntity.ok(sportClassService.getAllClasses());\n    }",
    "public ResponseEntity<List<SportClassResponseDTO>> getAllClasses(){\n        return ResponseEntity.ok(sportClassService.getAllClasses());\n    }\n" + controller_method
)

with open(controller_path, "w") as f:
    f.write(content)

