package com.fptu.swp391.sportscentermanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestJacksonLombok {
    @Test
    public void testDeserialization() throws Exception {
        String json = "{\n" +
                "    \"packageName\": \"Gois vv1\",\n" +
                "    \"durationDays\": 30,\n" +
                "    \"description\": \"Tập full thời gian\",\n" +
                "    \"price\": 699000.0,\n" +
                "    \"status\": null\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MembershipPackage pkg = mapper.readValue(json, MembershipPackage.class);
        System.out.println("======> STATUS IS: " + pkg.getStatus());
    }
}
