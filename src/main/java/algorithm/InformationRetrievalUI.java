package algorithm;

import com.sun.glass.events.KeyEvent;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;


import javax.swing.*;
import java.awt.event.ActionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Soamya Agrawal
 */
@Slf4j
public class InformationRetrievalUI extends javax.swing.JFrame {

    /* metaphone code implementation */
    // ABCDEFGHIJKLMNOPQRSTUVWXYZ
    private static final char[] DEFAULT_MAPPING = "vBKTvFKHvJKLMNvPKRSTvFW*YS".toCharArray();
    private static final String AEIOU = "AEIOU";

    private static char map(char c) {
        return DEFAULT_MAPPING[c - 'A'];
    }

    private static final int CODE_LENGTH = 6;

    private static String encode(final String string) {
        String word = string.toUpperCase();
        word = word.replaceAll("[^A-Z]", "");
        if (word.length() == 0) {
            return "";
        } else if (word.length() == 1) {
            return word;
        }
        word = word.replaceFirst("^[KGP]N", "N");
        word = word.replaceFirst("^WR", "R");
        word = word.replaceFirst("^AE", "E");
        word = word.replaceFirst("^PF", "F");
        word = word.replaceFirst("^WH", "W");
        word = word.replaceFirst("^X", "S");

        // Transform input string to all caps
        final char[] input = word.toCharArray();

        int codeIndex = 0;
        final char[] code = new char[CODE_LENGTH];

        // Save previous character of word
        char prevC = '?';

        for (int i = 0; i < input.length && codeIndex < CODE_LENGTH; i++) {
            final char c = input[i];
            /*
			 * if (c!='C' && c == prev_c) { 43 // prev_c = c is unncessary 44
			 * continue; 45 } 46
             */
            if (c == prevC) {
                // Especial rule for double letters
                if (c == 'C') {
                    // We have "cc". The first "c" has already been mapped
                    // to "K".
                    if (i < input.length - 1 && "EIY".indexOf(input[i + 1]) >= 0) {
                        // Do nothing and let it do to cc[eiy] -> KS
                    } else {
                        // This "cc" is just one sound
                        continue;
                    }
                } else {
                    // It is not "cc", so ignore the second letter
                    continue;
                }
            }
            switch (c) {

                case 'A':
                case 'E':
                case 'I':
                case 'O':
                case 'U':
                    // Keep a vowel only if it is the first letter
                    if (i == 0) {
                        code[codeIndex++] = c;
                    }
                    break;

                case 'F':
                case 'J':
                case 'L':
                case 'M':
                case 'N':
                case 'R':
                    code[codeIndex++] = c;
                    break;
                case 'Q':
                case 'V':
                case 'Z':
                    code[codeIndex++] = map(c);
                    break;

                // B -> B only if NOT MB$
                case 'B':
                    if (!(i == input.length - 1 && codeIndex > 0 && code[codeIndex - 1] == 'M')) {
                        code[codeIndex++] = c;
                    }
                    break;

                case 'C':
                    if (i < input.length - 2 && input[i + 1] == 'I' && input[i + 2] == 'A') {
                        code[codeIndex++] = 'X';
                    } else if (i < input.length - 1 && input[i + 1] == 'H' && i > 0 && input[i - 1] != 'S') {
                        code[codeIndex++] = 'X';
                    } else if (i < input.length - 1 && "EIY".indexOf(input[i + 1]) >= 0) {
                        code[codeIndex++] = 'S';
                    } else {
                        code[codeIndex++] = 'K';
                    }
                    break;

                case 'D':
                    if (i < input.length - 2 && input[i + 1] == 'G' && "EIY".indexOf(input[i + 2]) >= 0) {
                        code[codeIndex++] = 'J';
                    } else {
                        code[codeIndex++] = 'T';
                    }
                    break;

                case 'G':
                    // DG[IEY] -> D[IEY]
                    if (i < input.length - 1 && input[i + 1] == 'N' || i > 0 && input[i - 1] == 'D' && i < input.length - 1 && "EIY".indexOf(input[i + 1]) >= 0 || i < input.length - 1 && input[i + 1] == 'H'
                            && (i + 2 == input.length || AEIOU.indexOf(input[i + 2]) < 0)) {
                        // GN -> N [GNED -> NED]
                    }
                    else if (i < input.length - 1 && "EIY".indexOf(input[i + 1]) >= 0)
                            code[codeIndex++] = 'J';
                    else code[codeIndex++] = map(c);
                    break;
                case 'H':
                    if (i > 0 && "AEIOUCGPST".indexOf(input[i - 1]) >= 0) {
                        // vH -> v
                    }
                    else if (i < input.length - 1 && AEIOU.indexOf(input[i + 1]) < 0) {
                        // Hc -> c
                    }
                    else {
                        code[codeIndex++] = c;
                    }
                    break;

                case 'K':
                    if (i > 0 && input[i - 1] == 'C') {
                        // CK -> K
                    }
                    else {
                        code[codeIndex++] = map(c);
                    }
                    break;

                case 'P':
                    if (i < input.length - 1 && input[i + 1] == 'H') {
                        code[codeIndex++] = 'F';
                    } else {
                        code[codeIndex++] = map(c);
                    }
                    break;

                case 'S':
                    if (i < input.length - 2 && input[i + 1] == 'I' && (input[i + 2] == 'A' || input[i + 2] == 'O')) {
                        code[codeIndex++] = 'X';
                    } else if (i < input.length - 1 && input[i + 1] == 'H') {
                        code[codeIndex++] = 'X';
                    } else {
                        code[codeIndex++] = 'S';
                    }
                    break;

                case 'T':
                    // -TI[AO]- -> -XI[AO]-
                    // -TCH- -> -CH-
                    // -TH- -> -0-
                    // -T- -> -T-
                    if (i < input.length - 2 && input[i + 1] == 'I' && (input[i + 2] == 'A' || input[i + 2] == 'O')) {
                        code[codeIndex++] = 'X';
                    } else if (i < input.length - 1 && input[i + 1] == 'H') {
                        code[codeIndex++] = '0';
                    } else if (i < input.length - 2 && input[i + 1] == 'C' && input[i + 2] == 'H') {
                        // drop letter
                    }
                    else {
                        code[codeIndex++] = 'T';
                    }
                    break;

                case 'W':
                case 'Y':
                    // -Wv- -> -Wv-; -Wc- -> -c-
                    // -Yv- -> -Yv-; -Yc- -> -c-
                    if (i < input.length - 1 && AEIOU.indexOf(input[i + 1]) >= 0) {
                        code[codeIndex++] = map(c);
                    }
                    break;

                case 'X':
                    // -X- -> -KS-
                    code[codeIndex++] = 'K';
                    if (codeIndex < code.length) {
                        code[codeIndex++] = 'S';
                    }
                    break;

                default:
                    assert (false);
            }
            prevC = c;
        }
        return new String(code, 0, codeIndex);
    }

    private static HashMap<String, Double> sortByComparator(HashMap<String, Double> unsortMap, final boolean order) {

        List<Entry<String, Double>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, (Entry<String, Double> o1, Entry<String, Double> o2) -> {
            if (order) return o1.getValue().compareTo(o2.getValue());
            else return o2.getValue().compareTo(o1.getValue());
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Double> sortedMap = new LinkedHashMap<>();
        for (Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, Double> map) {
        for (Entry<String, Double> entry : map.entrySet()) {
            log.info("Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
    }

    /**
     * Creates new form algorithm.InformationRetrievalUI
     */
    public InformationRetrievalUI() throws IOException {
        initComponents();
        addPopup();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Search UI ");
        setPreferredSize(new java.awt.Dimension(639, 370));

        jTextField1.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        jTextField1.setToolTipText("Enter the Query Here");
        jTextField1.addActionListener((java.awt.event.ActionEvent evt) -> jTextField1ActionPerformed());
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jButton1.setText("Search");
        jButton1.addActionListener((java.awt.event.ActionEvent evt) -> jButton1ActionPerformed());

        jLabel1.setFont(new java.awt.Font("Ubuntu", 0, 13)); // NOI18N
        jLabel1.setText("Search Here");

        jLabel2.setFont(new java.awt.Font("Ubuntu", 0, 13)); // NOI18N
        jLabel2.setText("Search Results");

        jLabel3.setText("Select the no. of documents do be retrieved : ");

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(10, 3, 30, 1));

        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jList1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(jLabel2))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(271, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)))
                .addGap(34, 34, 34))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed() {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        dm.removeAllElements();
        String query = jTextField1.getText();
        try {
            searchResult(query);
        } catch (JSONException | IOException ex) {
            Logger.getLogger(InformationRetrievalUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1ActionPerformed() {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            dm.removeAllElements();
            String query = jTextField1.getText();
            try {
                searchResult(query);
            } catch (JSONException | IOException ex) {
                Logger.getLogger(InformationRetrievalUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jTextField1KeyPressed

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        // TODO add your handling code here:
        jList1.setSelectedIndex(jList1.locationToIndex(evt.getPoint()));
        data = jList1.getSelectedValue();
        int index = jList1.getSelectedIndex();
        log.info(data);
        if (SwingUtilities.isRightMouseButton(evt) && jList1.locationToIndex(evt.getPoint()) == index && !jList1.isSelectionEmpty()) {
            pop.show(jList1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jList1MouseClicked

    private String fileName = "/corpus/pizza_request_dataset.json";

    private void searchResult(String query) throws IOException {

        TreeMap<String, Integer> queryMap = new TreeMap<>();
        // Query Tokenization begins
        PTBTokenizer<CoreLabel> ptbtQuery = new PTBTokenizer<>(new StringReader(query), new CoreLabelTokenFactory(), "");
        while (ptbtQuery.hasNext()) {
            CoreLabel queryToken = ptbtQuery.next();
            // Query Stemming begins
            Stemmer s = new Stemmer();
            String querystring = queryToken.toString();
            querystring = querystring.toLowerCase();
            for (int c = 0; c < querystring.length(); c++) {
                s.add(querystring.charAt(c));
            }
            s.stem();
            String queryTerm;
            queryTerm = s.toString();
            if (queryTerm.matches("[a-zA-Z][a-z]+")) {

                // Query Metaphone begins
                queryTerm = encode(queryTerm);
            }
            Integer freq = queryMap.get(queryTerm);
            queryMap.put(queryTerm, (freq == null) ? 1 : freq + 1);
        }

        // Corpus-retrieving of documents from json file
        String json = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            json = sb.toString();
        } finally {
            br.close();
        }
        JSONArray jsonArray = new JSONArray(json);

        // 'finalTermFrequencyMap' is the TreeMap that displays the final document with dictionary
        // terms as tokens and integer value as document frequency
        TreeMap<String, Integer> finalTermFrequencyMap = new TreeMap<>();

        // Making an array list of all the individual Treemaps that represent
        // individual documents (in terms of tokens and term frequency).
        ArrayList<TreeMap<String, Integer>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String requestText = object.getString("request_text");

            //Document Tokenization begins
            TreeMap<String, Integer> individualTermFrequency = new TreeMap<>();

            PTBTokenizer<CoreLabel> ptbtDoc = new PTBTokenizer<>(new StringReader(requestText),
                    new CoreLabelTokenFactory(), "");
            while (ptbtDoc.hasNext()) {
                CoreLabel docToken = ptbtDoc.next();
                //Document Stemming begins
                Stemmer s = new Stemmer();
                String docString = docToken.toString();
                docString = docString.toLowerCase();

                for (int c = 0; c < docString.length(); c++) {
                    s.add(docString.charAt(c));
                }
                s.stem();
                String docTerm;
                docTerm = s.toString();
                if (docTerm.matches("[a-zA-Z][a-z]+")) {
                    //Document Metaphone begins
                    docTerm = encode(docTerm);
                }
                Integer freq = individualTermFrequency.get(docTerm);
                individualTermFrequency.put(docTerm, (freq == null) ? 1 : freq + 1);
            }
            for (Entry<String, Integer> entry : individualTermFrequency.entrySet()) {
                String key = entry.getKey();
                Integer freq = finalTermFrequencyMap.get(key);
                finalTermFrequencyMap.put(key, (freq == null) ? 1 : freq + 1);
            }

            list.add(individualTermFrequency);
        }
        //Total Number of Documents-'totalDocuments'
        int totalDocuments = list.size();
        TreeMap<String, Double> rankedProduct = new TreeMap<>();

        for (Entry<String, Integer> entry : finalTermFrequencyMap.entrySet()) {

            String key = entry.getKey();
            Integer documentFrequency = entry.getValue();
            Double rankedValue = (totalDocuments - documentFrequency + 0.5) / (documentFrequency + 0.5);
            rankedProduct.put(key, rankedValue);
        }

        // Making a HashMap that contains dictionary tokens and their final
        // product value which would be used to keep ranking of documents
        HashMap<String, Double> unsortMap = new HashMap<>();
        int i = 1;
        for (TreeMap<String, Integer> d : list) {
            Double product = 1.00;
            for (Entry<String, Integer> entry : queryMap.entrySet()) {

                String key = entry.getKey();
                if (d.containsKey(key)) {
                    product = product * (rankedProduct.get(key));

                }
            }
            unsortMap.put("Doc " + i, product);
            i++;
        }
        // Making a new HashMap that would sort the HashMap that contained key
        // and unsorted product ranks in descending order
        HashMap<String, Double> sortedMapDesc = sortByComparator(unsortMap, false);
        ArrayList<String> sortedOutput = new ArrayList<>();
        for (Entry<String, Double> entry : sortedMapDesc.entrySet()) {

            String key = entry.getKey();
            Double d = entry.getValue();
            sortedOutput.add(key);
            log.info(key + "   " + d);
        }

        populateList(sortedOutput);
    }

    private static String data = "";
    private final JPopupMenu pop = new JPopupMenu();
    private DefaultListModel<String> dm = new DefaultListModel<>();

    private void populateList(ArrayList<String> sortedOutput) {
        int docNumber = Integer.parseInt(jSpinner1.getValue().toString());
        for (int i = 0; i < docNumber; i++) {
            dm.addElement(sortedOutput.get(i));
        }
        jList1.setModel(dm);
    }

    private void addPopup() throws IOException {
        JMenuItem show = new JMenuItem("show");
        pop.add(show);
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }
        br.close();
        show.addActionListener((ActionEvent e) -> {
                try {
                    JSONArray array = new JSONArray(sb.toString());
                    JSONObject object = null;
                    String content = null;
                    if (data != null) {
                        object = array.getJSONObject(Integer.parseInt(data.substring(4)));
                        content = object.getString("request_text");
                    }
                    JOptionPane.showMessageDialog(InformationRetrievalUI.this, "<html><body><p style='width: 200px;'>" + content, "DOC DATA", JOptionPane.INFORMATION_MESSAGE);
                } catch (JSONException ex) {
                    Logger.getLogger(InformationRetrievalUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        );
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InformationRetrievalUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */

        java.awt.EventQueue.invokeLater(() -> {
            try {
                new InformationRetrievalUI().setVisible(true);
            } catch (JSONException | IOException ex) {
                Logger.getLogger(InformationRetrievalUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
