package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/certificates") @RequiredArgsConstructor
public class CertificateController {
    private final CertificateRepository certificates;
    private final Access access;
    private final CurrentUser me;
    private final ProgressService progress;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;

    /** Issue a certificate once the student has 100% progress in the course. */
    @PostMapping("/course/{courseId}") @PreAuthorize("hasRole('STUDENT')")
    public Certificate issue(@PathVariable Long courseId) {
        Course c = access.member(courseId);
        User u = me.get();
        return certificates.findByStudentAndCourse(u, c).orElseGet(() -> {
            if (progress.percent(u, c) < 100)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete 100% of the course to earn a certificate");
            return create(u, c);
        });
    }

    /** All of the student's certificates. Any finished course that has no certificate yet gets one automatically. */
    @GetMapping("/mine") @PreAuthorize("hasRole('STUDENT')")
    public List<Certificate> mine() {
        User u = me.get();
        for (Enrollment e : enrollments.findByStudent(u)) {
            Course c = e.getCourse();
            if (certificates.findByStudentAndCourse(u, c).isEmpty() && progress.percent(u, c) >= 100) create(u, c);
        }
        return certificates.findByStudent(u);
    }

    private Certificate create(User u, Course c) {
        Certificate cert = new Certificate();
        cert.setStudent(u);
        cert.setCourse(c);
        cert.setCode(UUID.randomUUID().toString());
        return certificates.save(cert);
    }

    /** Public: anyone can verify a certificate code. */
    @GetMapping("/verify/{code}")
    public Map<String, Object> verify(@PathVariable String code) {
        return certificates.findByCode(code)
                .map(c -> Map.<String, Object>of("valid", true, "student", c.getStudent().getName(),
                        "course", c.getCourse().getTitle(), "issuedAt", c.getIssuedAt().toString()))
                .orElse(Map.of("valid", false));
    }

    @GetMapping("/{code}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String code) throws Exception {
        Certificate c = certificates.findByCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!c.getStudent().getId().equals(me.get().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your certificate");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 50, 50, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 118, 110));
        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 38, new Color(30, 64, 175));
        Font big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 16);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        String adminName = users.findFirstByRole(Role.ADMIN).map(User::getName).orElse("Administrator");
        try {
            Image logo = Image.getInstance(new ClassPathResource("logo.png").getInputStream().readAllBytes());
            logo.scaleToFit(56, 56);
            logo.setAlignment(Image.ALIGN_CENTER);
            doc.add(logo);
        } catch (Exception ignored) { /* certificate still works without the logo */ }
        doc.add(centered("Learnly", brand, 6));
        doc.add(centered("CERTIFICATE OF COMPLETION", h1, 20));
        doc.add(centered("This certifies that", body, 12));
        doc.add(centered(c.getStudent().getName(), big, 12));
        doc.add(centered("has successfully completed the course", body, 12));
        doc.add(centered(c.getCourse().getTitle(), big, 20));
        doc.add(centered("Issued on " + c.getIssuedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), body, 15));
        doc.add(centered("Issued by: " + adminName + " (Admin)", body, 20));
        doc.add(centered("Verification code: " + c.getCode(), small, 0));
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate-" + c.getCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
    }

    private Paragraph centered(String text, Font f, float after) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(after);
        return p;
    }
}
