/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * scrubberGUI.java
 *
 * Created on 29 Dec, 2009, 4:27:10 PM
 */
package packages;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.io.*;
import java.util.Scanner;
import java.util.Arrays;
import java.awt.*;
import java.beans.*;

/**
 *
 * @author hari
 */
public class scrubberGUI extends javax.swing.JFrame implements PropertyChangeListener {

    /** Creates new form scrubberGUI */
    public scrubberGUI() {
        initComponents();
    }

    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */

        @Override
        public Void doInBackground() {
            setProgress(0);

            inputNumbers = new long[totalInputListNumbers];
            scrubNumbers = new long[totalScrubListNumbers];
            scrubRangeNumbers = new long[totalScrubRangeNumbers][2];
            outputNumbers = new long[totalInputListNumbers];
            int combinedInputCount = totalInputListNumbers + totalScrubListNumbers + totalScrubRangeNumbers;
            long tempForScrubRange = 0;
            boolean currentNumberWithinRange = false;

            // First get all the input into an array.
            scrubProgressBar.setIndeterminate(false);
            Scanner inputFileScanner = null;
            Scanner scrubFileScanner = null;
            Scanner scrubRangeFileScanner = null;
            try {
                inputFileScanner = new Scanner(new BufferedReader(new FileReader(inputFile)));
                scrubFileScanner = new Scanner(new BufferedReader(new FileReader(scrubFile)));
                if(scrubRangeEnableCheckBox.isSelected()) {
                    scrubRangeFileScanner = new Scanner(new BufferedReader(new FileReader(scrubRangeFile)));
                }

                statusLabel.setText("Loading data from Input List...");
                while (inputFileScanner.hasNext()) {
                    inputNumbers[inputNumberCount] = Long.parseLong(inputFileScanner.next());
                    long aa = inputNumbers[inputNumberCount];
                    inputNumberCount++;
                    setProgress(Math.min(((inputNumberCount) * 100) / (combinedInputCount), 100));
                }
                hasCompletedInputImport = true;

                statusLabel.setText("Loading data from Scrub List...");
                while (scrubFileScanner.hasNext()) {
                    scrubNumbers[scrubListNumberCount] = Long.parseLong(scrubFileScanner.next());
                    scrubListNumberCount++;
                    setProgress(Math.min(((inputNumberCount + scrubListNumberCount) * 100) / (combinedInputCount), 100));
                }
                hasCompletedScrubListImport = true;

                if(scrubRangeEnableCheckBox.isSelected()) {
                    statusLabel.setText("Loading data from Scrub-Range File...");
                    while (scrubRangeFileScanner.hasNext()) {
                        tempForScrubRange = Long.parseLong(scrubRangeFileScanner.next());
                        int lengthOfRange = 10 - (String.valueOf(tempForScrubRange).trim().length());
                        double rangeMultiplier = Math.pow(10, lengthOfRange);
                        double rangeAdder = Math.pow(10, (lengthOfRange)) - 1;
                        scrubRangeNumbers[scrubRangeNumberCount][0] = tempForScrubRange * (long)rangeMultiplier;
                        scrubRangeNumbers[scrubRangeNumberCount][1] = scrubRangeNumbers[scrubRangeNumberCount][0] + (long)rangeAdder;
                        scrubRangeNumberCount++;
                        setProgress(Math.min(((inputNumberCount + scrubListNumberCount + scrubRangeNumberCount) * 100) / (combinedInputCount), 100));
                    }
                    hasCompletedScrubRangeImport = true;
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
                statusLabel.setText("Error encountered when reading from files!");
            } finally {
                if (inputFileScanner != null) {
                    inputFileScanner.close();
                }
                if (scrubFileScanner != null) {
                    scrubFileScanner.close();
                }
            }
            setProgress(0);

            // Then sort the data in the array.
            statusLabel.setText("Sorting data for binary search operation...");
            scrubProgressBar.setIndeterminate(true);
            Arrays.sort(inputNumbers);
            Arrays.sort(scrubNumbers);

            // Then do the main exclusion processing.
            scrubProgressBar.setIndeterminate(false);
            setProgress(0);
            statusLabel.setText("Final processing...");
            for(inputNumberCount = 0; inputNumberCount < totalInputListNumbers; inputNumberCount++) {
                // Do, step by step.
                if(scrubRangeEnableCheckBox.isSelected()) {
                    currentNumberWithinRange = false;
                    for(scrubRangeNumberCount = 0; scrubRangeNumberCount < totalScrubRangeNumbers; scrubRangeNumberCount++) {
                        long a = inputNumbers[inputNumberCount];
                        long b = scrubRangeNumbers[scrubRangeNumberCount][0];
                        long c = scrubRangeNumbers[scrubRangeNumberCount][1];
                        if((inputNumbers[inputNumberCount] >= scrubRangeNumbers[scrubRangeNumberCount][0]) && (inputNumbers[inputNumberCount] <= scrubRangeNumbers[scrubRangeNumberCount][1])) {
                            currentNumberWithinRange = true;
                            break;
                        }
                    }
                    if(currentNumberWithinRange) {
                        setProgress(Math.min(((inputNumberCount + 1 * 100) / totalInputListNumbers), 100));
                        continue;
                    }
                }

                if (0 > Arrays.binarySearch(scrubNumbers, inputNumbers[inputNumberCount])) {
                    outputNumbers[outputNumberCount] = inputNumbers[inputNumberCount];
                    outputNumberCount++;
                }
                setProgress(Math.min((((inputNumberCount + 1) * 100) / totalInputListNumbers), 100));
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            done = true;
            if(!hasCompletedInputImport)
                statusLabel.setText("Importing of Input Numbers crashed at " + inputNumberCount + "-th line.");
            else if(!hasCompletedScrubListImport)
                statusLabel.setText("Importing of Scrub List crashed at " + scrubListNumberCount + "-th line.");
            else if(scrubRangeEnableCheckBox.isSelected() && !hasCompletedScrubRangeImport) {
                statusLabel.setText("Importing of Scrub Range crashed at " + scrubRangeNumberCount + "-th line.");
            }
            else {
                outputButton.setEnabled(true);
                statusLabel.setText("Number of MSISDN-s in output: " + outputNumberCount);
            }
            setCursor(null); // Turn off the wait cursor
        }
    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (0 == "progress".compareTo(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            scrubProgressBar.setValue(progress);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        inputListLabel = new javax.swing.JLabel();
        inputListButton = new javax.swing.JButton();
        scrubListLabel = new javax.swing.JLabel();
        scrubListButton = new javax.swing.JButton();
        outputLabel = new javax.swing.JLabel();
        outputButton = new javax.swing.JButton();
        scrubButton = new javax.swing.JButton();
        scrubLabel = new javax.swing.JLabel();
        scrubProgressBar = new javax.swing.JProgressBar();
        inputListDoneCheckBox = new javax.swing.JCheckBox();
        scrubListDoneCheckBox = new javax.swing.JCheckBox();
        statusSeperator = new javax.swing.JSeparator();
        statusLabel = new javax.swing.JLabel();
        scrubRangeLabel = new javax.swing.JLabel();
        scrubRangeButton = new javax.swing.JButton();
        scrubRangeDoneCheckBox = new javax.swing.JCheckBox();
        scrubRangeEnableCheckBox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Scrubber v3");
        setAlwaysOnTop(true);

        inputListLabel.setText("1. Choose an Input List of Numbers:");

        inputListButton.setText("Select Input List");
        inputListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputListButtonActionPerformed(evt);
            }
        });

        scrubListLabel.setText("2. Select a list of numbers for scrubing:");

        scrubListButton.setText("Select Scrub List");
        scrubListButton.setEnabled(false);
        scrubListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrubListButtonActionPerformed(evt);
            }
        });

        outputLabel.setText("4. Save the Output File:");

        outputButton.setText("Save Output File");
        outputButton.setEnabled(false);
        outputButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputButtonActionPerformed(evt);
            }
        });

        scrubButton.setText("Scrub!");
        scrubButton.setEnabled(false);
        scrubButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrubButtonActionPerformed(evt);
            }
        });

        scrubLabel.setText("3. Intiate Scrubbing:");

        inputListDoneCheckBox.setEnabled(false);

        scrubListDoneCheckBox.setEnabled(false);

        statusLabel.setText("Waiting for user to select files...");

        scrubRangeLabel.setText("Optional. Choose a Black-list Range File:");

        scrubRangeButton.setText("Select Black-list Range File");
        scrubRangeButton.setEnabled(false);
        scrubRangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrubRangeButtonActionPerformed(evt);
            }
        });

        scrubRangeDoneCheckBox.setEnabled(false);

        scrubRangeEnableCheckBox.setEnabled(false);
        scrubRangeEnableCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrubRangeEnableCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusSeperator, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(outputLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(outputButton, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE))
                    .addComponent(scrubProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE)
                    .addComponent(statusLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(scrubRangeEnableCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrubRangeLabel))
                            .addComponent(scrubListLabel)
                            .addComponent(inputListLabel)
                            .addComponent(scrubLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(scrubButton, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                                .addGap(26, 26, 26))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(inputListButton, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(inputListDoneCheckBox))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(scrubListButton, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrubListDoneCheckBox))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(scrubRangeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrubRangeDoneCheckBox)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(inputListLabel)
                                        .addComponent(inputListButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addComponent(inputListDoneCheckBox))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(scrubListLabel))
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(scrubListButton))))
                            .addComponent(scrubListDoneCheckBox))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(scrubRangeEnableCheckBox)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(scrubRangeLabel)
                                .addComponent(scrubRangeButton))))
                    .addComponent(scrubRangeDoneCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scrubLabel)
                    .addComponent(scrubButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrubProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputLabel)
                    .addComponent(outputButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(statusSeperator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void inputListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputListButtonActionPerformed
        // Handle open button action.
        int returnVal = inputFileChooser.showOpenDialog(scrubberGUI.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            inputListDoneCheckBox.setSelected(true);
            scrubListButton.setEnabled(true);
            inputFile = inputFileChooser.getSelectedFile();
            try {
                totalInputListNumbers = countLines(inputFile);
            } catch (IOException ex) {
                Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            statusLabel.setText("Input file contains " + totalInputListNumbers + " numbers. Waiting for DND file selection...");
        } else {
            statusLabel.setText("Input file selection cancelled... Waiting for input...");
        }
    }//GEN-LAST:event_inputListButtonActionPerformed

    private void scrubListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrubListButtonActionPerformed
        // Handle open button action.
        int returnVal = scrubFileChooser.showOpenDialog(scrubberGUI.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            scrubListDoneCheckBox.setSelected(true);
            scrubButton.setEnabled(true);
            scrubRangeEnableCheckBox.setEnabled(true);
            scrubFile = scrubFileChooser.getSelectedFile();
            try {
                totalScrubListNumbers = countLines(scrubFile);
            } catch (IOException ex) {
                Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            statusLabel.setText("DND file contains " + (totalScrubListNumbers) + " numbers. Press 'Scrub!' to begin.");
        } else {
            statusLabel.setText("DND file selection cancelled... Waiting for input...");
        }
    }//GEN-LAST:event_scrubListButtonActionPerformed

    private void scrubButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrubButtonActionPerformed

        // Define basics for progressbar
        scrubProgressBar.setStringPainted(true);
        scrubProgressBar.setValue(0);

        // Reset the scrub button
        scrubButton.setEnabled(false);
        inputListButton.setEnabled(false);
        scrubListButton.setEnabled(false);
        scrubRangeButton.setEnabled(false);
        scrubRangeEnableCheckBox.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        //Instances of javax.swing.SwingWorker are not reusuable, so
        //we create new instances as needed.
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }//GEN-LAST:event_scrubButtonActionPerformed

    private void outputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputButtonActionPerformed
        int returnVal = outputFileChooser.showSaveDialog(scrubberGUI.this);
        BufferedWriter outputStream = null;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            statusLabel.setText("Saving to file...");
            File outputFile = outputFileChooser.getSelectedFile();
            try {
                // Save output to selected file.
                outputStream = new BufferedWriter(new FileWriter(outputFile));
                for (int c = 0; c < outputNumberCount; c++) {
                    outputStream.write(outputNumbers[c] + "\r\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            statusLabel.setText("Done! Restart this program to do more scrubbing! :-)");
        } else {
            statusLabel.setText("Save cancelled. Waiting for input...");
        }
    }//GEN-LAST:event_outputButtonActionPerformed

    private void scrubRangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrubRangeButtonActionPerformed
        // Handle open button action.
        int returnVal = scrubRangeFileChooser.showOpenDialog(scrubberGUI.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            scrubRangeDoneCheckBox.setSelected(true);
            scrubButton.setEnabled(true);
            scrubRangeEnableCheckBox.setEnabled(true);
            hasCompletedscrubRangeFileSelection = true;
            scrubRangeFile = scrubRangeFileChooser.getSelectedFile();
            try {
                totalScrubRangeNumbers = countLines(scrubRangeFile);
            } catch (IOException ex) {
                Logger.getLogger(scrubberGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            statusLabel.setText("Scrub-Ranges file contains " + totalScrubRangeNumbers + " numbers. Press 'Scrub!' to begin.");
        } else {
            statusLabel.setText("Scrub-Ranges file selection cancelled... Waiting for input...");
        }
    }//GEN-LAST:event_scrubRangeButtonActionPerformed

    private void scrubRangeEnableCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrubRangeEnableCheckBoxActionPerformed
        if(scrubRangeEnableCheckBox.isSelected()) {
          scrubRangeButton.setEnabled(true);
          if(!hasCompletedscrubRangeFileSelection)
              scrubButton.setEnabled(false);
        }
        else {
          scrubRangeButton.setEnabled(false);
          if(!hasCompletedscrubRangeFileSelection)
              scrubButton.setEnabled(true);
        }
    }//GEN-LAST:event_scrubRangeEnableCheckBoxActionPerformed

    public static int countLines(File someFile) throws IOException {
        int numberOfLines = 0;
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new FileReader(someFile));
            String l;
            while ((l = inputStream.readLine()) != null) {
                numberOfLines++;
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return numberOfLines;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new scrubberGUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton inputListButton;
    private javax.swing.JCheckBox inputListDoneCheckBox;
    private javax.swing.JLabel inputListLabel;
    private javax.swing.JButton outputButton;
    private javax.swing.JLabel outputLabel;
    private javax.swing.JButton scrubButton;
    private javax.swing.JLabel scrubLabel;
    private javax.swing.JButton scrubListButton;
    private javax.swing.JCheckBox scrubListDoneCheckBox;
    private javax.swing.JLabel scrubListLabel;
    private javax.swing.JProgressBar scrubProgressBar;
    private javax.swing.JButton scrubRangeButton;
    private javax.swing.JCheckBox scrubRangeDoneCheckBox;
    private javax.swing.JCheckBox scrubRangeEnableCheckBox;
    private javax.swing.JLabel scrubRangeLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JSeparator statusSeperator;
    // End of variables declaration//GEN-END:variables
    final JFileChooser inputFileChooser = new JFileChooser();
    final JFileChooser scrubFileChooser = new JFileChooser();
    final JFileChooser outputFileChooser = new JFileChooser();
    final JFileChooser scrubRangeFileChooser = new JFileChooser();

    int totalInputListNumbers = 0;
    int totalScrubListNumbers = 0;
    int totalScrubRangeNumbers = 0;
    int outputNumberCount = 0;

    File inputFile = null;
    File scrubFile = null;
    File scrubRangeFile = null;

    long[] inputNumbers;
    long[] scrubNumbers;
    long[][] scrubRangeNumbers;
    long[] outputNumbers;

    private Task task;

    boolean done = false;
    boolean hasCompletedInputImport = false;
    boolean hasCompletedScrubListImport = false;
    boolean hasCompletedScrubRangeImport = false;
    boolean hasCompletedscrubRangeFileSelection = false;

    int inputNumberCount = 0;
    int scrubListNumberCount = 0;
    int scrubRangeNumberCount = 0;

}
