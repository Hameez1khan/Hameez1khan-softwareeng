// Copyright 2017 Sebastian Kuerten
//
// This file is part of OpenMetroMaps.
//
// OpenMetroMaps is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// OpenMetroMaps is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with OpenMetroMaps. If not, see <http://www.gnu.org/licenses/>.

package org.openmetromaps.maps.editor;

import de.topobyte.awt.util.GridBagConstraintsEditor;
import org.openmetromaps.maps.ReplacementServices;
import org.openmetromaps.maps.graph.NetworkLine;
import org.openmetromaps.maps.graph.Node;
import org.openmetromaps.maps.model.Line;
import org.openmetromaps.maps.model.Station;
import org.openmetromaps.maps.model.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SelectionPanel extends JPanel {

    final static Logger logger = LoggerFactory.getLogger(SelectionPanel.class);

    private static final long serialVersionUID = 1L;

    private MapEditor mapEditor;

    private List<Node> nodes = new ArrayList<>();

    private final Set<Line> selectedLines = new HashSet<>();
    private final Set<Line> relatedLines = new HashSet<>();

    public SelectionPanel(MapEditor mapEditor) {
        super(new GridLayout(0, 1));
        this.mapEditor = mapEditor;

        setupLayout();

        mapEditor.addDataChangeListener(this::refresh);
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraintsEditor ce = new GridBagConstraintsEditor();
        GridBagConstraints c = ce.getConstraints();

        ce.fill(GridBagConstraints.BOTH);
        ce.weight(0, 0);
        c.insets = new Insets(0, 4, 4, 4);

        int lineCount = 0;

        // Stations

        for (Node node : nodes) {
            JLabel stationLabel = new JLabel(node.station.getName());
            ce.gridPos(0, lineCount++);
            panel.add(stationLabel, c);
        }

        // Lines related to stations

        JPanel linesPanel = new JPanel(new GridLayout(0, Math.max(relatedLines.size(), 1), 4, 2));
        linesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        for (Line line : relatedLines) {
            JLabel lineLabel = new JLabel(line.getName(), SwingConstants.CENTER);
            lineLabel.setOpaque(true);
            lineLabel.setBackground(Color.decode(line.getColor()));
            linesPanel.add(lineLabel);
        }
        for (Line line : relatedLines) {
            JCheckBox lineCheckbox = new JCheckBox();
            lineCheckbox.setSelected(selectedLines.contains(line));
            lineCheckbox.addActionListener(a -> {
                if (lineCheckbox.isSelected())
                    selectedLines.add(line);
                else
                    selectedLines.remove(line);
                mapEditor.triggerDataChanged();
            });
            linesPanel.add(lineCheckbox);
        }
        ce.gridPos(0, lineCount++);
        panel.add(linesPanel, c);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 0, 0, 2));

        JButton closeButton = new JButton("Close");
        closeButton.setEnabled(closeButtonEnabled());
        closeButton.addActionListener(e -> {
            Node node = nodes.iterator().next();
            ReplacementServices.closeStation(mapEditor.getModel().getData(), node.station, new ArrayList<>(selectedLines));
            mapEditor.triggerMapChanged();
        });
        ce.gridPos(0, lineCount);
        buttonPanel.add(closeButton);

        JButton replacementButton = new JButton("Replacement");
        replacementButton.setEnabled(replacementButtonEnabled());
        replacementButton.addActionListener(e -> {
            List<Line> lines = new ArrayList<>(selectedLines);

            // choose an arbitrary line from the selection (they all share the selected stations)
            // this MUST exist because the button is disabled otherwise
            Line line = lines.get(0);
            List<Station> stations = line.getStops().stream()
                .map(Stop::getStation)
                .collect(Collectors.toList());

            List<Station> selectedStationsInOrder = nodes.stream()
                .sorted((s1, s2) -> {
                    int idx1 = stations.indexOf(s1.station);
                    int idx2 = stations.indexOf(s2.station);
                    return Integer.compare(idx1, idx2);
                })
                .map(n -> n.station)
                .collect(Collectors.toList());

            List<Station> selectedStationsInOriginalOrder = nodes.stream()
                .map(n -> n.station)
                .collect(Collectors.toList());

            Station boundaryStation1 = selectedStationsInOrder.getFirst();
            Station boundaryStation2 = selectedStationsInOrder.getLast();
            if(selectedStationsInOriginalOrder.indexOf(boundaryStation1) > selectedStationsInOriginalOrder.indexOf(boundaryStation2)) {
                Collections.reverse(selectedStationsInOrder);
            }

            ReplacementServices.createReplacementService(mapEditor.getModel().getData(), selectedStationsInOrder, lines);
            mapEditor.triggerMapChanged();
        });
        ce.gridPos(1, lineCount);
        buttonPanel.add(replacementButton);

        JButton alternativeButton = new JButton("Alternative");
        alternativeButton.setEnabled(alternativeButtonEnabled());
        alternativeButton.addActionListener(e -> {
            Iterator<Node> iterator = nodes.iterator();
            Station stationA = iterator.next().station;
            Station stationB = iterator.next().station;
            ReplacementServices.createAlternativeService(mapEditor.getModel().getData(), stationA, stationB);
            mapEditor.triggerMapChanged();
        });
        ce.gridPos(2, lineCount);
        buttonPanel.add(alternativeButton);

        ce.gridPos(0, lineCount++);
        panel.add(buttonPanel, c);

        ce.gridPos(0, lineCount);
        ce.weight(1, 1);
        panel.add(new JPanel(), c);

        add(new JScrollPane(panel));
    }

    private boolean closeButtonEnabled() {
        return nodes.size() == 1 && !selectedLines.isEmpty();
    }

    private boolean replacementButtonEnabled() {
        if (nodes.size() < 2)
            return false;

        if (selectedLines.isEmpty())
            return false;

        return selectedLines.stream().allMatch(line -> {
            List<Station> stations = line.getStops().stream().map(Stop::getStation).collect(Collectors.toList());
            List<Integer> indexes = nodes.stream().map(n -> stations.indexOf(n.station)).sorted().collect(Collectors.toList());
            return indexes.get(indexes.size() - 1) - indexes.get(0) == indexes.size() - 1; // assuming no duplicates
        });
    }

    private boolean alternativeButtonEnabled() {
        return nodes.size() == 2;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        relatedLines.clear();
        for (Node node : nodes) {
            node.edges.forEach(e -> relatedLines.addAll(e.lines.stream().map(nl -> nl.line).collect(Collectors.toSet())));
        }
        selectedLines.retainAll(relatedLines);
        refresh();
    }

    protected void refresh() {
        removeAll();
        setupLayout();
        revalidate();
        repaint();
    }
}