package hr.fipu.footmash.ai;

import java.util.List;

public class GeminiRequest {
    private List<Content> contents;

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {
        private String role;
        private List<Part> parts;

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() { return role; }
        public List<Part> getParts() { return parts; }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }
}
