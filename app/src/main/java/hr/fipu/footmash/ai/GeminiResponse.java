package hr.fipu.footmash.ai;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public static class Candidate {
        private Content content;

        public Content getContent() {
            return content;
        }
    }

    public static class Content {
        private List<Part> parts;
        private String role;

        public List<Part> getParts() {
            return parts;
        }
        
        public String getRole() {
            return role;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }
    }

    public String getResponseText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null) {
                if (!firstCandidate.getContent().getParts().isEmpty()) {
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
        }
        return "Nema odgovora";
    }
}
