package view;

import javax.swing.SwingUtilities;
import core.NetworkDealer;
import core.SidePanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.FriendReview;
import model.friend.Friend;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import util.CustomBibtexField;
import util.Loader;

/**
 * 0.1 | 13/6/2009
 * + Add rating
 * + Add icons
 * Hack add-on to Entry Editor
 * @author Thien Rong
 */
public class MyEntryEditor extends JPanel implements PropertyChangeListener, ImageConstants {

    String rated = "You have rated ";
    String unrated = "You have not rated yet.";
    JButton btnViewReviews = new JButton();
    JLabel lblRating = new JLabel();
    JLabel lblPeersWithItem = new JLabel();
    JRating ratings = new JRating(CustomBibtexField.MAX_RATING);
    SidePanel main;
    //
    BibtexEntry entry;
    BasePanel bp;

    public MyEntryEditor(final SidePanel main) {
        this.main = main;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(main.getName()));
        JButton btnPeer = new JButton("Refresh", new ImageIcon(Loader.get(RELOAD)));
        btnPeer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println("getting peer reviews");
                forceRefreshReview();
            }
        });

        JPanel pnlCtrl = new JPanel(new GridLayout(0, 1));
        pnlCtrl.add(btnPeer);
        pnlCtrl.add(ratings);
        pnlCtrl.add(lblRating);
        pnlCtrl.add(lblPeersWithItem);
        pnlCtrl.add(btnViewReviews);

        this.add(pnlCtrl, BorderLayout.NORTH);

        ratings.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                CustomBibtexField.setRating(bp, entry, ratings.getRating());
            }
        });
    }

    public void setReviews() {
        if(entry == null){
            return; 
        }
        // to calculate avg rating (including self)
        int totalRating = 0;
        int numRating = 0;
        // update own ratings first, if no BUID => not shared yet
        int rating = CustomBibtexField.getRating(entry);
        String isRatedStr = unrated; // default for unrated
        if (rating != 0) {
            totalRating += rating;
            numRating++;
            isRatedStr = rated + rating;
        }

        // get from peer if shared and got BUID
        String BUID = CustomBibtexField.getBUID(entry);
        boolean hasReview = false;
        if (BUID != null) {
            Map<String, FriendReview> reviews = main.getFriendReviewsModel().getReviews(BUID);

            if (reviews != null) {

                String myReview = entry.getField("review");
                final Map<String, String> diffReviews = new TreeMap<String, String>();
                // for all meaningful review OR rating
                for (Map.Entry<String, FriendReview> review : reviews.entrySet()) {
                    // compare all the peerReview and only display those different from mine
                    FriendReview peerReview = review.getValue();
                    String peerReviewStr = peerReview.getReview();
                    if (CustomBibtexField.isDiffReview(myReview, peerReviewStr)) {
                        diffReviews.put(review.getKey(), peerReviewStr);
                    }

                    // incr ratings
                    if (peerReview.getRating() != 0) {
                        totalRating += peerReview.getRating();
                        numRating++;
                    }
                }
                // remove ex-listener
                for (ActionListener actionListener : btnViewReviews.getActionListeners()) {
                    btnViewReviews.removeActionListener(actionListener);
                }
                btnViewReviews.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        ReviewDialog dialog = new ReviewDialog(entry, bp);
                        for (Map.Entry<String, String> entry : diffReviews.entrySet()) {
                            dialog.handleChange(entry.getValue(), entry.getKey());
                        }
                        dialog.setVisible(true);
                    }
                });
                btnViewReviews.setText(diffReviews.size() + " diff reviews");
                btnViewReviews.setVisible(diffReviews.size() != 0);
                lblPeersWithItem.setText(reviews.size() + " peers with item");

                hasReview = true;
            }
        }

        int finalRating = 0;
        if (numRating != 0) {
            finalRating = Math.round(totalRating / numRating);
        }
        //System.out.println(totalRating + "/" + numRating);
        lblRating.setText("<html>" + numRating + " ratings<br/>" +
                isRatedStr + "</html>");
        ratings.setRating(finalRating, false);

        if (hasReview == false) {
            btnViewReviews.setVisible(false);
            lblPeersWithItem.setText("No Reviews found");
        }
    }

    public void setEntry(BibtexEntry entry, BasePanel bp) {
        this.entry = entry;
        this.bp = bp;

        try {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setReviews();
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void forceRefreshReview() {
        NetworkDealer d = main.getDealer();
        if (d != null) {
            for (Friend f : main.getDealer().getConnectedFriends()) {
                main.getDealer().sendBrowseRequest(f.getFUID(), null);
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setReviews();
    }
}