package com.wimn.utilities;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Universal Audio Accessibility and Voice-Over Engine for Desktop JavaFX & System Intimations.
 * Provides high-clarity synthesized audio sound effects and native Windows speech synthesis
 * supporting Executive Female, Tactical Female, and Robotic Male personas.
 */
public class AudioAccessibilityManager {
    private static final Logger LOGGER = Logger.getLogger(AudioAccessibilityManager.class.getName());
    private static final AudioAccessibilityManager INSTANCE = new AudioAccessibilityManager();

    public enum VoicePersona {
        FEMALE_EXECUTIVE("Executive Female Voice", "Warm, articulate & natural studio female narration", true, 0),
        FEMALE_TACTICAL("Cyber Tactical Female Voice", "Crisp, fast operational dispatch voice announcer", true, 2),
        MALE_ROBOTIC("Cyber Robotic AI Voice", "Monotone synthesized digitized vocoder cadence", false, 1);

        private final String displayName;
        private final String description;
        private final boolean isFemale;
        private final int speechRate; // -10 to +10 for System.Speech

        VoicePersona(String displayName, String description, boolean isFemale, int speechRate) {
            this.displayName = displayName;
            this.description = description;
            this.isFemale = isFemale;
            this.speechRate = speechRate;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public boolean isFemale() { return isFemale; }
        public int getSpeechRate() { return speechRate; }
    }

    private VoicePersona currentPersona = VoicePersona.FEMALE_EXECUTIVE;
    private boolean voiceOverEnabled = true;
    private boolean soundFxEnabled = true;

    private final ExecutorService audioExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "WIMN-AudioAccessibility");
        t.setDaemon(true);
        return t;
    });

    private Process activeSpeechProcess = null;

    private AudioAccessibilityManager() {}

    public static AudioAccessibilityManager getInstance() {
        return INSTANCE;
    }

    public synchronized VoicePersona getCurrentPersona() {
        return currentPersona;
    }

    public synchronized void setCurrentPersona(VoicePersona persona) {
        if (persona != null) {
            this.currentPersona = persona;
            LOGGER.info("Audio Accessibility Voice Persona updated to: " + persona.getDisplayName());
        }
    }

    public synchronized boolean isVoiceOverEnabled() {
        return voiceOverEnabled;
    }

    public synchronized void setVoiceOverEnabled(boolean enabled) {
        this.voiceOverEnabled = enabled;
        if (!enabled) {
            cancelSpeech();
        }
    }

    public synchronized boolean isSoundFxEnabled() {
        return soundFxEnabled;
    }

    public synchronized void setSoundFxEnabled(boolean enabled) {
        this.soundFxEnabled = enabled;
    }

    /**
     * Speaks the given text using the active voice-over persona.
     */
    public void speak(String text) {
        if (!voiceOverEnabled || text == null || text.isBlank()) {
            return;
        }

        final String cleanText = cleanMarkdownForSpeech(text);
        final VoicePersona persona = getCurrentPersona();

        audioExecutor.submit(() -> {
            cancelSpeech();

            try {
                // Escape quotes for PowerShell
                String escapedText = cleanText.replace("\"", "`\"").replace("'", "''");

                String psScript = String.format(
                        "$ErrorActionPreference = 'SilentlyContinue'; " +
                        "Add-Type -AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$installed = $synth.GetInstalledVoices(); " +
                        "if (%s) { " +
                        "  $target = $installed | Where-Object { $_.VoiceInfo.Gender -eq 'Female' -or $_.VoiceInfo.Name -match 'Zira|Hazel|Jenny|Aria|Samantha|Victoria' } | Select-Object -First 1; " +
                        "} else { " +
                        "  $target = $installed | Where-Object { $_.VoiceInfo.Gender -eq 'Male' -or $_.VoiceInfo.Name -match 'David|George|Mark' } | Select-Object -First 1; " +
                        "} " +
                        "if ($target) { $synth.SelectVoice($target.VoiceInfo.Name); } " +
                        "$synth.Rate = %d; " +
                        "$synth.Volume = 100; " +
                        "$synth.Speak('%s');",
                        persona.isFemale() ? "$true" : "$false",
                        persona.getSpeechRate(),
                        escapedText
                );

                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", psScript);
                synchronized (this) {
                    activeSpeechProcess = pb.start();
                }
                activeSpeechProcess.waitFor();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Voice synthesis intimation: " + e.getMessage());
            }
        });
    }

    /**
     * Cancels any active speech readout immediately.
     */
    public synchronized void cancelSpeech() {
        if (activeSpeechProcess != null && activeSpeechProcess.isAlive()) {
            try {
                activeSpeechProcess.destroyForcibly();
            } catch (Exception ignored) {}
            activeSpeechProcess = null;
        }
    }

    /**
     * Plays a pleasant rising chime for successful operations (pure synthesized audio, 0 external files).
     */
    public void playSuccessTone() {
        if (!soundFxEnabled) return;
        audioExecutor.submit(() -> {
            try {
                playPcmTone(523, 70, 0.25f);  // C5
                playPcmTone(659, 70, 0.25f);  // E5
                playPcmTone(784, 80, 0.28f);  // G5
                playPcmTone(1046, 120, 0.32f); // C6
            } catch (Exception ignored) {}
        });
    }

    /**
     * Plays a dual warning alert tone.
     */
    public void playWarningTone() {
        if (!soundFxEnabled) return;
        audioExecutor.submit(() -> {
            try {
                playPcmTone(320, 100, 0.35f);
                playPcmTone(220, 140, 0.35f);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Plays a subtle cybernetic interaction click.
     */
    public void playClickTone() {
        if (!soundFxEnabled) return;
        audioExecutor.submit(() -> {
            try {
                playPcmTone(1200, 25, 0.15f);
            } catch (Exception ignored) {}
        });
    }

    private void playPcmTone(int freqHz, int durationMs, float amplitude) {
        try {
            float sampleRate = 44100f;
            int numSamples = (int) (sampleRate * (durationMs / 1000f));
            byte[] buffer = new byte[numSamples];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / freqHz);
                // Apply smooth envelope to prevent clicking
                float envelope = 1.0f;
                if (i < 80) envelope = i / 80.0f;
                else if (i > numSamples - 160) envelope = (numSamples - i) / 160.0f;

                short sample = (short) (Math.sin(angle) * 127.0 * amplitude * envelope);
                buffer[i] = (byte) sample;
            }

            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception e) {
            // Audio line unavailable
        }
    }

    private String cleanMarkdownForSpeech(String markdown) {
        if (markdown == null) return "";
        return markdown
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("#+\\s*", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "") // remove emoji surrogate pairs
                .replaceAll("[•—_~]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
