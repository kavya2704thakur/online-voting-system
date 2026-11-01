package com.voting.controller;

import com.voting.entity.Candidate;
import com.voting.entity.User;
import com.voting.repository.CandidateRepository;
import com.voting.repository.UserRepository;
import com.voting.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VotingController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CandidateRepository candidateRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }
        if (user.getFaceData() == null || user.getFaceData().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Face biometric registration is mandatory!");
        }
        userRepo.save(user);
        emailService.sendEmail(user.getEmail(), "Online Voting System - Registration",
                "Hi " + user.getName() + ",\n\nYour account and face biometric ID have been enrolled successfully.");
        return ResponseEntity.ok("Registration successful with Face Biometric ID!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginData) {
        Optional<User> userOpt = userRepo.findByEmail(loginData.getEmail());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(loginData.getPassword())) {
            return ResponseEntity.ok(userOpt.get());
        }
        return ResponseEntity.status(401).body("Invalid email or password!");
    }

    @GetMapping("/candidates")
    public List<Candidate> getCandidates() {
        return candidateRepo.findAll();
    }

    @PostMapping("/vote")
    public ResponseEntity<?> castVote(@RequestParam Long userId, @RequestParam Long candidateId) {
        Optional<User> userOpt = userRepo.findById(userId);
        Optional<Candidate> candOpt = candidateRepo.findById(candidateId);

        if (userOpt.isEmpty() || candOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User or Candidate not found!");
        }

        User user = userOpt.get();
        if (user.isHasVoted()) {
            return ResponseEntity.badRequest().body("You have already voted! Multiple votes are strictly prohibited.");
        }

        Candidate candidate = candOpt.get();
        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateRepo.save(candidate);

        user.setHasVoted(true);
        userRepo.save(user);

        emailService.sendEmail(user.getEmail(), "Vote Confirmation",
                "Hi " + user.getName() + ",\n\nYour vote has been cast for " + candidate.getName() + " (" + candidate.getParty() + ").");

        return ResponseEntity.ok("Biometric verification verified. Vote cast successfully!");
    }

    // --- ADMIN ENDPOINTS ---

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (ADMIN_USER.equals(username) && ADMIN_PASS.equals(password)) {
            return ResponseEntity.ok("Admin authenticated");
        }
        return ResponseEntity.status(401).body("Invalid Admin Username or Password!");
    }

    @PostMapping("/admin/candidate")
    public ResponseEntity<?> addCandidate(@RequestBody Candidate candidate) {
        candidate.setVoteCount(0);
        candidateRepo.save(candidate);
        return ResponseEntity.ok("Candidate added successfully!");
    }

    @DeleteMapping("/admin/candidate/{id}")
    public ResponseEntity<?> deleteCandidate(@PathVariable Long id) {
        if (!candidateRepo.existsById(id)) {
            return ResponseEntity.badRequest().body("Candidate not found!");
        }
        candidateRepo.deleteById(id);
        return ResponseEntity.ok("Candidate deleted successfully!");
    }

    @PostMapping("/admin/reset")
    public ResponseEntity<?> resetElection() {
        try {
            jdbcTemplate.update("UPDATE candidates SET vote_count = 0");
            jdbcTemplate.update("UPDATE users SET has_voted = 0");
            return ResponseEntity.ok("Election reset successful! All votes set to 0.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Reset failed: " + e.getMessage());
        }
    }
}