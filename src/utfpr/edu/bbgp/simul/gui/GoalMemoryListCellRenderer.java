/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.simul.gui;

import utfpr.edu.swing.utils.ColorUtil;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import utfpr.edu.bbgp.agent.GoalMemory;

/**
 *
 * @author henri
 */
public class GoalMemoryListCellRenderer extends JLabel implements ListCellRenderer<GoalMemory>{
    private final float blendProp = 0.85f;

    public GoalMemoryListCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends GoalMemory> list, GoalMemory itemGM, int index, boolean isSelected, boolean hasFocus) {
        setText(itemGM.toString());
        
        boolean negative = getText().contains("did not became");
        
        if(isSelected){
            setBackground((negative ? ColorUtil.blend(list.getSelectionBackground(), Color.RED, blendProp) : ColorUtil.blend(list.getSelectionBackground(), Color.GREEN, blendProp)));
            setForeground(list.getSelectionForeground());
        }else{
            setBackground((negative ? ColorUtil.blend(list.getBackground(), Color.RED, blendProp) : ColorUtil.blend(list.getBackground(), Color.GREEN, blendProp)));
            setForeground(list.getForeground());
        }
        
        setFont(list.getFont());
        
        setEnabled(list.isEnabled());
        
        if(hasFocus){
            setBorder(BorderFactory.createLineBorder(list.getBackground().darker()));
        }else{
            setBorder(null);
        }
        
        return this;
    }
    
}
