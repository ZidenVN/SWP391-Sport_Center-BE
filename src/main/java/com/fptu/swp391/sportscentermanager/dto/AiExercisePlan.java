package com.fptu.swp391.sportscentermanager.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Buổi tập AI gợi ý. Vừa là schema structured output gửi cho Claude, vừa là dữ liệu trả về FE
 * (bộ luật dự phòng cũng điền đúng cấu trúc này).
 */
public record AiExercisePlan(
    @JsonPropertyDescription("Tên ngắn cho buổi tập, tiếng Việt, ví dụ 'Đốt mỡ toàn thân - mức cơ bản'")
    String title,
    @JsonPropertyDescription("1-2 câu tóm tắt vì sao buổi tập phù hợp với học viên")
    String summary,
    @JsonPropertyDescription("Các bước khởi động, mỗi phần tử một câu ngắn")
    List<String> warmup,
    @JsonPropertyDescription("Danh sách bài tập chính theo thứ tự thực hiện")
    List<Exercise> exercises,
    @JsonPropertyDescription("Các bước giãn cơ / thả lỏng sau buổi tập")
    List<String> cooldown,
    @JsonPropertyDescription("Lưu ý an toàn và lời khuyên cho HLV / học viên")
    List<String> advice
) {
    public record Exercise(
        @JsonPropertyDescription("Tên bài tập tiếng Việt, có thể kèm tên tiếng Anh trong ngoặc")
        String name,
        @JsonPropertyDescription("Số hiệp, ví dụ '3'")
        String sets,
        @JsonPropertyDescription("Số lần hoặc thời gian mỗi hiệp, ví dụ '12 lần' hoặc '40 giây'")
        String reps,
        @JsonPropertyDescription("Thời gian nghỉ giữa hiệp, ví dụ '60 giây'")
        String rest,
        @JsonPropertyDescription("Ghi chú kỹ thuật ngắn")
        String note
    ) {}
}
