import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.swing.*;
// import javafx.application.Application;
// import javafx.scene.media.Media;
// import javafx.scene.media.MediaPlayer;
// import javafx.stage.Stage;

public class AudioPlayer {
    private Clip audioClip;
    private boolean isLooping = false;
    private boolean isPaused = false;
    private ArrayList<File> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private JLabel songLabel;
    private JList<String> songList;
    private JLabel songImageLabel;
    private JSlider timeSlider;
    private JLabel timeLabel;
    private Timer timer;
    private FloatControl gainControl;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AudioPlayer());
    }

    public AudioPlayer() {
        JFrame frame = new JFrame("HI DD MUSIC PLAYER");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("HI DD MUSIC PLAYER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 136, 229));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        frame.add(titleLabel, BorderLayout.NORTH);

        songList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(songList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Playlist"));
        songList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && songList.getSelectedIndex() != -1) {
                currentIndex = songList.getSelectedIndex();
                loadAudio(playlist.get(currentIndex));
                playAudio(false);
            }
        });

        songImageLabel = new JLabel();
        songImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        songImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        songImageLabel.setPreferredSize(new Dimension(200, 200));
        songImageLabel.setIcon(new ImageIcon("default-image.jpg"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, songImageLabel);
        splitPane.setDividerLocation(300);
        frame.add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        songLabel = new JLabel("No song playing", SwingConstants.CENTER);
        songLabel.setFont(new Font("Arial", Font.BOLD, 14));
        songLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        bottomPanel.add(songLabel);

        timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        timeSlider.setEnabled(false);
        timeSlider.addChangeListener(e -> {
            if (timeSlider.getValueIsAdjusting() && audioClip != null) {
                long position = (long) (timeSlider.getValue() * 1000);
                audioClip.setMicrosecondPosition(position);
            }
        });

        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        timeLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.add(timeSlider, BorderLayout.CENTER);
        timePanel.add(timeLabel, BorderLayout.EAST);

        bottomPanel.add(timePanel);

        JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, -80, 6, 0);
        volumeSlider.setUI(new javax.swing.plaf.basic.BasicSliderUI(volumeSlider) {
            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(30, 136, 229));
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }

            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(200, 200, 200));
                g2d.fillRect(trackRect.x, trackRect.y + trackRect.height / 3, trackRect.width, trackRect.height / 3);
                g2d.setColor(new Color(30, 136, 229));
                int fillWidth = (int) ((volumeSlider.getValue() - volumeSlider.getMinimum()) / (double) (volumeSlider.getMaximum() - volumeSlider.getMinimum()) * trackRect.width);
                g2d.fillRect(trackRect.x, trackRect.y + trackRect.height / 3, fillWidth, trackRect.height / 3);
            }
        });

        volumeSlider.addChangeListener(e -> {
            if (gainControl != null) {
                float value = volumeSlider.getValue();
                gainControl.setValue(value);
            }
        });

        JPanel volumePanel = new JPanel();
        volumePanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        //volumePanel.setBackground(Color.WHITE);

        ImageIcon vol = new ImageIcon("./media/volumeicon.png");
        ImageIcon resize = new ImageIcon(vol.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        JLabel iconLabel = new JLabel(resize);

        volumeSlider.setPreferredSize(new Dimension(150, 20)); // Increase slider width
        volumeSlider.setOpaque(false);
        volumePanel.add(iconLabel);
        volumePanel.add(volumeSlider);
        volumePanel.setPreferredSize(new Dimension(frame.getWidth(), 40));

        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(volumePanel);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton prevButton = createButton("⏮");
        JButton playButton = createButton("▶️");
        JButton pauseButton = createButton("⏸");
        JButton nextButton = createButton("⏭");
        JButton loopButton = createButton("🔁");
        JButton stopButton = createButton("⏹");

        controlPanel.add(prevButton);
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(nextButton);
        controlPanel.add(loopButton);
        controlPanel.add(stopButton);
        bottomPanel.add(controlPanel);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        openMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    playlist.add(file);
                }
                updatePlaylistDisplay();
            }
        });

        playButton.addActionListener(e -> playAudio(false));
        pauseButton.addActionListener(e -> pauseAudio());
        stopButton.addActionListener(e -> stopAudio());
        loopButton.addActionListener(e -> playAudio(true));
        prevButton.addActionListener(e -> playPrevious());
        nextButton.addActionListener(e -> playNext());

        frame.setVisible(true);

        ImageIcon image = new ImageIcon("./media/WhatsApp Image 2021-12-15 at 17.05.33.jpeg");
        frame.setIconImage(image.getImage());
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        return button;
    }

    private void loadAudio(File audioFile) {
        try {
            stopAudio();
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);

            if (audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
            }

            isPaused = false;
            updateSongLabel();
            updateSongImage(audioFile);
            timeSlider.setEnabled(true);
            timeSlider.setValue(0);
            timeSlider.setMaximum((int) (audioClip.getMicrosecondLength() / 1000));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load audio file: " + e.getMessage());
        }
    }

    private void playAudio(boolean loop) {
        if (audioClip != null) {
            if (isPaused) {
                audioClip.start();
                isPaused = false;
            } else {
                audioClip.setFramePosition(0);
                if (loop) {
                    isLooping = true;
                    audioClip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    isLooping = false;
                    audioClip.start();
                }
            }
            updateSongLabel();
            if (timer == null) {
                timer = new Timer(100, e -> {
                    if (audioClip.isRunning()) {
                        int currentMilliseconds = (int) (audioClip.getMicrosecondPosition() / 1000);
                        int totalMilliseconds = (int) (audioClip.getMicrosecondLength() / 1000);
                        timeSlider.setValue(currentMilliseconds);

                        // Update the time label
                        timeLabel.setText(formatTime(currentMilliseconds) + " / " + formatTime(totalMilliseconds));
                    }
                });
            }
            timer.start();
        } else {
            JOptionPane.showMessageDialog(null, "No audio file loaded.");
        }
    }

    private void pauseAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            isPaused = true;
            updateSongLabel();
            if (timer != null) timer.stop();
        }
    }

    private void stopAudio() {
        if (audioClip != null) {
            isLooping = false;
            isPaused = false;
            audioClip.stop();
            audioClip.setFramePosition(0);
            songLabel.setText("No song playing");
            timeSlider.setValue(0);
            timeSlider.setEnabled(false);
            timeLabel.setText("00:00 / 00:00");
            if (timer != null) timer.stop();
        }
    }

    private void playPrevious() {
        if (!playlist.isEmpty() && currentIndex > 0) {
            currentIndex--;
            loadAudio(playlist.get(currentIndex));
            playAudio(false);
        }
    }

    private void playNext() {
        if (!playlist.isEmpty() && currentIndex < playlist.size() - 1) {
            currentIndex++;
            loadAudio(playlist.get(currentIndex));
            playAudio(false);
        }
    }

    private void updateSongLabel() {
        if (!playlist.isEmpty() && currentIndex >= 0) {
            String songName = playlist.get(currentIndex).getName();
            songLabel.setText((isPaused ? "Paused: " : isLooping ? "Looping: " : "Playing: ") + songName);
        } else {
            songLabel.setText("No song playing");
        }
    }

    private void updateSongImage(File audioFile) {
        songImageLabel.setIcon(new ImageIcon("default-image.jpg"));
    }

    private void updatePlaylistDisplay() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (File file : playlist) listModel.addElement(file.getName());
        songList.setModel(listModel);
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        int hours = milliseconds / (1000 * 60 * 60);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
