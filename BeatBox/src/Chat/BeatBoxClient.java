package Chat;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBoxClient {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    JList incomingList;
    JTextField userMessage;
    int nextNum;
    Vector<String> listVector = new Vector<>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    Sequence mySequence = null;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
            "Hand Clap" , "High Tom" , "Hi Bong" , "Maracas" , "Whistle" , "Low Conga" , "Cowbell", "Vibraslap", "Low-mid Tom",
            "High Agogo", "Open Hi Congo"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args){

        new BeatBoxClient().startUp(args[0]);
    }

    public void startUp(String name)
    {
        userName = name;

        try{
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (Exception ex) {
            System.out.println("Couldn't connect");
        }

        setUpMidi();
        buildGUI();
    }

    public void buildGUI() {

        theFrame = new JFrame("BeatBox " + userName);
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        // Buttons

        JButton start = new JButton("Start", new ImageIcon("C:\\Users\\kalash\\IdeaProjects\\BeatBox\\res\\start.gif"));
        start.setMaximumSize(new Dimension(150, 40));
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);


        JButton stop = new JButton("Stop", new ImageIcon("C:\\Users\\kalash\\IdeaProjects\\BeatBox\\res\\stop.jpg"));
        stop.setMaximumSize(new Dimension(150, 40));
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);


        JButton upTempo = new JButton("Tempo Up");
        upTempo.setMaximumSize(new Dimension(150, 40));
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);


        JButton downTempo = new JButton("Tempo Down");
        downTempo.setMaximumSize(new Dimension(150, 40));
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton save = new JButton("Save");
        save.setMaximumSize(new Dimension(150, 40));
        save.addActionListener(new MySaveListener());
        buttonBox.add(save);

        JButton loadSong = new JButton("Load Song");
        loadSong.setMaximumSize(new Dimension(150, 40));
        loadSong.addActionListener(new MyReadInListener());
        buttonBox.add(loadSong);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        JButton sendIt = new JButton("Send");
        sendIt.setMaximumSize(new Dimension(150, 40));
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);  // ArrayList
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    } // Built Gui

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);

        } catch (Exception e) {e.printStackTrace();}
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<>();

            for (int j = 0; j < 16; j++) {

                JCheckBox jc = checkboxList.get(j + 16*i);
                if(jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(key);
                } else {
                    trackList.add(0);
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }

        track.add(makeEvent(192,9,1,0,15));

        try{

            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {e.printStackTrace();}
    } //builtTrackAndStart

    // Button Function

    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a)
        {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a)
        {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a)
        {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a)
        {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }

    public class MySaveListener implements ActionListener{
        public void actionPerformed(ActionEvent a)
        {
            boolean[] checkboxState = new boolean[256];

            for (int i = 0; i < 256; i++) {

                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if(check.isSelected())
                {
                    checkboxState[i] = true;
                }
            }

            try{
                FileOutputStream fileStream = new FileOutputStream(new File("D:\\SongTrack\\song.ser"));
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class MyReadInListener implements ActionListener{
        public void actionPerformed(ActionEvent a) {

            boolean[] checkboxStates = null;
            try{
                FileInputStream fileIn = new FileInputStream(new File("D:\\SongTrack\\song.ser"));
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxStates = (boolean[]) is.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                assert checkboxStates != null : " File is Null ";
                check.setSelected(checkboxStates[i]);         // if true check selected = true else false
            }

            sequencer.stop();
            buildTrackAndStart();
        }
    }

    public class MySendListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            boolean[] checkBoxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if(check.isSelected())
                {
                    checkBoxState[i] = true;
                }
            }

            String messageToSend = null;
            try{
                out.writeObject(userName + " " + nextNum++ + ": " + userMessage.getText());
                out.writeObject(checkBoxState);
            } catch (Exception ex){
                System.out.println("Couldn't send to server");
            }
            userMessage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener{
        public void valueChanged(ListSelectionEvent le){
            if(!le.getValueIsAdjusting()){
                String selected = (String) incomingList.getSelectedValue();
                if(selected != null){
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class RemoteReader implements Runnable{
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;
        public void run() {
            try{
                while((obj = in.readObject()) != null){
                    System.out.println("Got an object from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public class MyPlayMineListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if(mySequence != null)
            {
                sequence = mySequence;
            }
        }
    }

    public void changeSequence(boolean[] checkboxState)
    {
        for (int i = 0; i <256; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if(checkboxState[i]){
                check.setSelected(true);
            } else{
                check.setSelected(false);
            }
        }
    }

    public void makeTracks(ArrayList<Integer> list)
    {
        Iterator<Integer> it = list.iterator();
        for (int i = 0; i < 16; i++) {
            Integer num = (Integer)it.next();

            if(num != 0)
            {
                int numKey = num.intValue();
                track.add(makeEvent(144,9,numKey,100,i));   // note on
                track.add(makeEvent(144,9,numKey,100,i+1));  // note off
            }
            /*
            else{
                System.out.println("num == 0");
            }
            */
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick)
    {
        MidiEvent event = null;

        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {e.printStackTrace();}

        return event;
    }
}
