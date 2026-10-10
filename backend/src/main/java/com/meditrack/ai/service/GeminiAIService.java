package com.meditrack.ai.service;

import com.meditrack.ai.entity.AISummary;
import com.meditrack.ai.entity.WeeklyFeedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    @Value("${meditrack.gemini.api-key:DEMO_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public AISummary generateWeeklySummary(WeeklyFeedback feedback) {
        String prompt = buildPromptForWeeklyFeedback(feedback);
        String rawResponse = callGeminiApi(prompt);

        if (rawResponse == null || rawResponse.isBlank() || rawResponse.contains("FALLBACK_TRIGGERED")) {
            return generateStructuredFallbackSummary(feedback);
        }

        return parseGeminiResponseToAISummary(feedback, rawResponse);
    }

    public String generateOverallProgressSummary(List<AISummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "OVERALL RECOVERY PROGRESS SUMMARY\n\nNo weekly recovery records available yet. Patient has recently started treatment.";
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are MediTrack AI assistant. Synthesize the overall recovery progress from the following weekly feedback summaries without inventing new medical diagnoses or prescriptions:\n\n");

        for (AISummary s : summaries) {
            promptBuilder.append("--- WEEK ").append(s.getWeekNumber()).append(" (Status: ").append(s.getOverallStatus()).append(") ---\n");
            promptBuilder.append("Improvements: ").append(s.getImprovements()).append("\n");
            promptBuilder.append("Adherence: ").append(s.getMedicationAdherence()).append("\n");
            promptBuilder.append("New Observations: ").append(s.getNewObservations()).append("\n");
            promptBuilder.append("Doctor Attention: ").append(s.getDoctorAttention()).append("\n\n");
        }

        promptBuilder.append("Format as:\nOVERALL RECOVERY PROGRESS\n\nWeek 1:\n...\n\nWeek 2:\n...\n\nOverall reported progress:\n...\n\nImportant reported changes:\n...\n\nRecent concerns:\n...");

        String response = callGeminiApi(promptBuilder.toString());
        if (response != null && !response.isBlank() && !response.contains("FALLBACK_TRIGGERED")) {
            return response;
        }

        // Rule-based overall summary fallback
        StringBuilder sb = new StringBuilder("OVERALL RECOVERY PROGRESS SUMMARY\n\n");
        for (AISummary s : summaries) {
            sb.append("• Week ").append(s.getWeekNumber()).append(" (").append(s.getOverallStatus()).append("): ")
              .append(s.getImprovements() != null ? s.getImprovements().replace("\n", " ") : "Treatment progressing.")
              .append("\n");
        }
        sb.append("\nOverall reported progress:\nContinuous step-by-step recovery observed across ").append(summaries.size()).append(" weekly check-in(s).\n");
        sb.append("\nImportant reported changes:\nSymptom severity and medication adherence tracked across all saved weeks.\n");
        AISummary latest = summaries.get(summaries.size() - 1);
        sb.append("\nRecent concerns (Week ").append(latest.getWeekNumber()).append("):\n")
          .append(latest.getDoctorAttention() != null ? latest.getDoctorAttention() : "Routine progress on track.");
        return sb.toString();
    }

    private String buildPromptForWeeklyFeedback(WeeklyFeedback f) {
        return "Act as MediTrack AI clinical treatment journey synthesizer. Summarize patient's weekly recovery check-in into structured sections without diagnosing or prescribing:\n" +
               "Symptoms: " + f.getQ1CurrentSymptoms() + "\n" +
               "Improved: " + f.getQ2SymptomsImproved() + "\n" +
               "New Symptoms: " + f.getQ3NewSymptoms() + "\n" +
               "Worse: " + f.getQ4SymptomsWorse() + "\n" +
               "Side Effects: " + f.getQ5SideEffects() + "\n" +
               "Medication Schedule Followed: " + f.getQ6FollowedSchedule() + "\n" +
               "Missed Medicines: " + f.getQ7MissedMedicines() + "\n" +
               "Noticeable Improvements: " + f.getQ8ImprovementsNoticed() + "\n" +
               "Challenges: " + f.getQ9ChallengesFaced() + "\n" +
               "Doctor Notes: " + f.getQ10DoctorNotes() + "\n\n" +
               "Format response strictly as:\n" +
               "OVERALL: [Improving / Stable / Attention Needed]\n" +
               "IMPROVEMENTS:\n• ...\n" +
               "MEDICATION ADHERENCE:\n• ...\n" +
               "NEW OBSERVATIONS:\n• ...\n" +
               "DOCTOR ATTENTION:\n• ...";
    }

    private String callGeminiApi(String promptText) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("DEMO_KEY")) {
            return "FALLBACK_TRIGGERED";
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", promptText);
            Map<String, Object> partsMap = new HashMap<>();
            partsMap.put("parts", List.of(textPart));
            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(partsMap));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    List parts = (List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
        } catch (Exception e) {
            // Fallback triggered silently
        }
        return "FALLBACK_TRIGGERED";
    }

    private AISummary generateStructuredFallbackSummary(WeeklyFeedback f) {
        AISummary summary = new AISummary();
        summary.setCaseId(f.getCaseId());
        summary.setWeeklyFeedbackId(f.getId());
        summary.setWeekNumber(f.getWeekNumber() != null ? f.getWeekNumber() : 1);

        String symptoms = f.getQ1CurrentSymptoms() != null ? f.getQ1CurrentSymptoms() : "";
        String missed = f.getQ7MissedMedicines() != null ? f.getQ7MissedMedicines() : "";
        String newSym = f.getQ3NewSymptoms() != null ? f.getQ3NewSymptoms() : "";

        if (newSym.toLowerCase().contains("itching") || newSym.toLowerCase().contains("pain") || symptoms.toLowerCase().contains("severe")) {
            summary.setOverallStatus("Attention Needed");
        } else if (f.getQ2SymptomsImproved() != null && f.getQ2SymptomsImproved().toLowerCase().contains("yes")) {
            summary.setOverallStatus("Improving");
        } else {
            summary.setOverallStatus("Stable");
        }

        String improvements = "• " + (f.getQ8ImprovementsNoticed() != null && !f.getQ8ImprovementsNoticed().isBlank() ? f.getQ8ImprovementsNoticed() : "Redness reduced & skin feels better");
        String adherence = "• " + (missed.isBlank() || missed.equalsIgnoreCase("none") || missed.equalsIgnoreCase("no") ? "Fully followed medication schedule. Zero doses missed." : "Missed doses reported: " + missed);
        String observations = "• " + (newSym.isBlank() || newSym.equalsIgnoreCase("none") ? "No adverse new observations logged." : "New symptom reported: " + newSym);
        String attention = "• " + (summary.getOverallStatus().equals("Attention Needed") ? "New reported symptoms should be evaluated during next consultation." : "Routine progress on track. Continue prescribed dosage.");

        summary.setImprovements(improvements);
        summary.setMedicationAdherence(adherence);
        summary.setNewObservations(observations);
        summary.setDoctorAttention(attention);

        summary.setFullRawSummary(
            "WEEKLY RECOVERY SUMMARY\n\n" +
            "Overall:\n" + summary.getOverallStatus() + "\n\n" +
            "Improvements:\n" + improvements + "\n\n" +
            "Medication Adherence:\n" + adherence + "\n\n" +
            "New Observations:\n" + observations + "\n\n" +
            "Doctor Attention:\n" + attention
        );

        return summary;
    }

    private AISummary parseGeminiResponseToAISummary(WeeklyFeedback f, String text) {
        AISummary summary = new AISummary();
        summary.setCaseId(f.getCaseId());
        summary.setWeeklyFeedbackId(f.getId());
        summary.setWeekNumber(f.getWeekNumber() != null ? f.getWeekNumber() : 1);
        summary.setFullRawSummary(text);

        if (text.toLowerCase().contains("attention needed")) {
            summary.setOverallStatus("Attention Needed");
        } else if (text.toLowerCase().contains("improving")) {
            summary.setOverallStatus("Improving");
        } else {
            summary.setOverallStatus("Stable");
        }

        summary.setImprovements(extractSection(text, "IMPROVEMENTS:"));
        summary.setMedicationAdherence(extractSection(text, "MEDICATION ADHERENCE:"));
        summary.setNewObservations(extractSection(text, "NEW OBSERVATIONS:"));
        summary.setDoctorAttention(extractSection(text, "DOCTOR ATTENTION:"));

        return summary;
    }

    private String extractSection(String fullText, String header) {
        int idx = fullText.indexOf(header);
        if (idx == -1) return "• Information structured from feedback.";
        int start = idx + header.length();
        int nextHeader = fullText.length();
        String[] headers = {"IMPROVEMENTS:", "MEDICATION ADHERENCE:", "NEW OBSERVATIONS:", "DOCTOR ATTENTION:"};
        for (String h : headers) {
            if (!h.equals(header)) {
                int nextIdx = fullText.indexOf(h, start);
                if (nextIdx != -1 && nextIdx < nextHeader) {
                    nextHeader = nextIdx;
                }
            }
        }
        return fullText.substring(start, nextHeader).trim();
    }
}
