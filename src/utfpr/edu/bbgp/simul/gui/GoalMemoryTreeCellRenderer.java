/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import utfpr.edu.swing.utils.ColorUtil;

/**
 *
 * @author henri
 */
public class GoalMemoryTreeCellRenderer extends DefaultTreeCellRenderer {

    private final float blendProp = 0.85f;

    public GoalMemoryTreeCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component base = super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, hasFocus);
        if (!(value instanceof DefaultMutableTreeNode)) {
            return base;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (!(node.getUserObject() instanceof TreeExplanationEntry)) {
            return base;
        }

        TreeExplanationEntry entry = (TreeExplanationEntry) node.getUserObject();

        setText(entry.toString());
        
        boolean negative = getText().contains("did not became");

        if (entry.getType() == TreeExplanationEntry.GOAL_TYPE) {
            if (isSelected) {
                setBackground((negative ? ColorUtil.blend(getBackgroundSelectionColor(), Color.RED, blendProp) : ColorUtil.blend(getBackgroundSelectionColor(), Color.GREEN, blendProp)));
            } else {
                setBackground((negative ? ColorUtil.blend(getBackgroundNonSelectionColor(), Color.RED, blendProp) : ColorUtil.blend(getBackgroundNonSelectionColor(), Color.GREEN, blendProp)));
            }
        }else{
            setBackground(ColorUtil.blend((isSelected ? getBackgroundSelectionColor() : getBackgroundNonSelectionColor()), Color.white, 1.0f));
        }

        if (isSelected) {
            setBorder(BorderFactory.createLineBorder(getBorderSelectionColor()));
        } else {
            setBorder(null);
        }

        return this;
    }

}
