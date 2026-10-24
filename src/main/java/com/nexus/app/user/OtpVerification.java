package com.nexus.app.user;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document (collection = "otp_verifications")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class OtpVerification {
    @Id 
    private String id;
    private String email;
    private String otp;
    private LocalDateTime expirationTime;
    private boolean verified;
}
