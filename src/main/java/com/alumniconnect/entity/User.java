package com.alumniconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Integer batchYear;
    private String classSection;
    private String rollNumber;

    // Academic
    private Integer graduationYear;
    private String stream;
    private String specialization;
    private String university;
    private String degree;
    private String schoolAchievements;
    private String extracurricular;

    // Employment
    private String jobTitle;
    private String company;
    private String industry;
    private boolean businessOwner;
    private String businessName;
    private String skills;

    // Social
    private String linkedin;
    private String twitter;
    private String github;
    private String instagram;
    private String facebook;
    private String personalWebsite;
    private String telegram;

    // Contact
    private String phoneNumber;
    private String whatsappNumber;
    private boolean phoneVisible = true;
    private boolean whatsappVisible = true;

    // Location
    private String currentCity;
    private String currentCountry;
    private String hometown;

    // Bio and profile photos
    @Column(length = 500)
    private String bio;
    private String profilePhotoUrl;
    private String coverPhotoUrl;

    private boolean enabled = false;
    private boolean verified = false;
    private boolean suspended = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {
    }

    public User(String fullName, String email, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(Integer batchYear) {
        this.batchYear = batchYear;
    }

    public String getClassSection() {
        return classSection;
    }

    public void setClassSection(String classSection) {
        this.classSection = classSection;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getSchoolAchievements() {
        return schoolAchievements;
    }

    public void setSchoolAchievements(String schoolAchievements) {
        this.schoolAchievements = schoolAchievements;
    }

    public String getExtracurricular() {
        return extracurricular;
    }

    public void setExtracurricular(String extracurricular) {
        this.extracurricular = extracurricular;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public boolean isBusinessOwner() {
        return businessOwner;
    }

    public void setBusinessOwner(boolean businessOwner) {
        this.businessOwner = businessOwner;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getPersonalWebsite() {
        return personalWebsite;
    }

    public void setPersonalWebsite(String personalWebsite) {
        this.personalWebsite = personalWebsite;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public boolean isPhoneVisible() {
        return phoneVisible;
    }

    public void setPhoneVisible(boolean phoneVisible) {
        this.phoneVisible = phoneVisible;
    }

    public boolean isWhatsappVisible() {
        return whatsappVisible;
    }

    public void setWhatsappVisible(boolean whatsappVisible) {
        this.whatsappVisible = whatsappVisible;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }

    public String getCurrentCountry() {
        return currentCountry;
    }

    public void setCurrentCountry(String currentCountry) {
        this.currentCountry = currentCountry;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getCoverPhotoUrl() {
        return coverPhotoUrl;
    }

    public void setCoverPhotoUrl(String coverPhotoUrl) {
        this.coverPhotoUrl = coverPhotoUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getProfileCompletion() {
        int filled = 0;
        int total = 12;
        if (fullName != null && !fullName.isBlank()) filled++;
        if (email != null && !email.isBlank()) filled++;
        if (phoneNumber != null && !phoneNumber.isBlank()) filled++;
        if (currentCity != null && !currentCity.isBlank()) filled++;
        if (jobTitle != null && !jobTitle.isBlank()) filled++;
        if (company != null && !company.isBlank()) filled++;
        if (university != null && !university.isBlank()) filled++;
        if (profilePhotoUrl != null && !profilePhotoUrl.isBlank()) filled++;
        if (linkedin != null && !linkedin.isBlank()) filled++;
        if (bio != null && !bio.isBlank()) filled++;
        if (graduationYear != null) filled++;
        if (stream != null && !stream.isBlank()) filled++;
        return (filled * 100) / total;
    }
}
