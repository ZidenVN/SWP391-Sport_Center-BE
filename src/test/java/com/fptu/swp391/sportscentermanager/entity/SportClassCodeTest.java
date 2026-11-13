package com.fptu.swp391.sportscentermanager.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SportClassCodeTest {

    private static String codeOf(String subjectName, Long id) {
        SportClass c = SportClass.builder().classId(id)
            .subject(subjectName == null ? null : Subject.builder().subjectName(subjectName).build()).build();
        c.assignCodeIfMissing();
        return c.getClassCode();
    }

    @Test
    void code_usesUnaccentedSubjectPrefixAndPaddedId() {
        assertEquals("YOG-0012", codeOf("Yoga", 12L));
        assertEquals("BOI-0003", codeOf("Bơi Lội", 3L));
        assertEquals("DIE-0007", codeOf("Điền kinh", 7L));
        assertEquals("LOP-0005", codeOf("Võ", 5L));      // dưới 3 chữ cái
        assertEquals("LOP-0001", codeOf(null, 1L));
    }

    @Test
    void code_isNeverChangedOnceAssigned() {
        SportClass c = SportClass.builder().classId(12L).subject(Subject.builder().subjectName("Yoga").build()).build();
        c.assignCodeIfMissing();
        c.setSubject(Subject.builder().subjectName("Boxing").build());
        c.assignCodeIfMissing();
        assertEquals("YOG-0012", c.getClassCode());
    }

    @Test
    void displayName_usesCodeOrFallsBackToId() {
        SportClass c = SportClass.builder().classId(12L).subject(Subject.builder().subjectName("Yoga").build()).build();
        assertEquals("Yoga #12", c.displayName());   // lớp cũ chưa có mã
        c.assignCodeIfMissing();
        assertEquals("Yoga YOG-0012", c.displayName());
    }

    @Test
    void code_waitsForId() {
        SportClass c = SportClass.builder().subject(Subject.builder().subjectName("Yoga").build()).build();
        c.assignCodeIfMissing();
        assertNull(c.getClassCode());
    }
}
