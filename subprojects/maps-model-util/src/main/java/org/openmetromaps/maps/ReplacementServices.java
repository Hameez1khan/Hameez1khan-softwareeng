package org.openmetromaps.maps;

import java.util.ArrayList;
import java.util.List;

import org.openmetromaps.maps.model.Line;
import org.openmetromaps.maps.model.ModelData;
import org.openmetromaps.maps.model.Station;
import org.openmetromaps.maps.model.Stop;

public class ReplacementServices {

    private ReplacementServices(){}
    
    public static final String COLOR_STRING = "#009EE3";


    public static int calculateReplacementCost(ModelData model, Line line) {
        throw new RuntimeException("Not implemented");
    
    }
    
    public static void closeStation(ModelData model, Station station, List<Line> lines) {
        
        List<Stop> lineStops;
        List<Stop> stationStops = station.getStops();
    
        for(Line line : lines){
            lineStops = line.getStops();
            if(lineStops.size() < 3) return;
        }
        //removes the stop from the station
        for(Line line : lines){
    
            lineStops = line.getStops();
    
            int stopIndex = MapModelUtil.findStop(lineStops , station.getName());
            if(lineStops.size() < 3 || stopIndex == -1) return;
    
            stationStops.remove(lineStops.get(stopIndex));
    
        }
    
        //removes the stop from the line
        for(Line line : lines){
    
            lineStops = line.getStops();
    
            int stopIndex = MapModelUtil.findStop(lineStops , station.getName());
            if(lineStops.size() < 3 || stopIndex == -1 ) return;
            
            lineStops.remove(stopIndex);

        }



        if(station.getStops().isEmpty()){
            model.stations.remove(station);
        }
        
    }
    

    

    public static void createReplacementService(ModelData model, List<Station> stations, List<Line> lines) {
        
        List<Station> arrangedStations;
        
        arrangedStations = arrangeStations(stations, lines);
    
        if( arrangedStations.isEmpty() || !areConsecutiveStations(arrangedStations, stations)  || areTerminalStations(arrangedStations, lines) || arrangedStations.get(0).equals(arrangedStations.get(arrangedStations.size()-1))
        ) return;

        boolean create = true;
        
        for(Line line: lines){

            boolean reversed = stationReversed(stations , arrangedStations);
            int primaryStationIndex = MapModelUtil.findStop(line.getStops(), arrangedStations.get(0).getName());
            int secondaryStationIndex = MapModelUtil.findStop(line.getStops(), arrangedStations.get(arrangedStations.size()-1).getName());
            
            //checking if a the selected in a particular line is a terminal station
            if(oneStationIsTerminal(line, arrangedStations)){

                boolean primarySectionBoundaryTerminal = checkIfTerminal(line, arrangedStations.get(0));

                Line line1 = createLine(model,line, primaryStationIndex , secondaryStationIndex , primarySectionBoundaryTerminal );
                
                model.lines.add(line1);

                if(create){
                    Line replacementLine = createReplacementLine(model, lines, line, primaryStationIndex, secondaryStationIndex,true,primarySectionBoundaryTerminal);
                    model.lines.add(replacementLine);
                }

            }

            else
            {
                // when both of the stations are not terminal stations

                
                Line line1 = createLineWhenNoTerminal(model, line, reversed,primaryStationIndex , secondaryStationIndex, true);
                Line line2 = createLineWhenNoTerminal(model, line, reversed,primaryStationIndex , secondaryStationIndex, false);
                
                model.lines.add(line1);
                
                model.lines.add(line2);

                if(create){
                    Line replacementLine = createReplacementLine(model, lines,  line, primaryStationIndex, secondaryStationIndex,false,false);
                    model.lines.add(replacementLine);
                }
            }
            create = false;
            
        }
        removeAllLines(lines , model);

    }


    private static Line createLine(ModelData model, Line line, int primaryStationIndex,int secondaryStationIndex, boolean primarySectionBoundaryTerminal ){
        int lineID = generateID(model);
        
        Line newLine = new Line(lineID, line.getName(),line.getColor(),false,new ArrayList<>());


        if(primarySectionBoundaryTerminal)
            for(int index = secondaryStationIndex ; index < line.getStops().size(); index++){
                newLine.getStops().add(line.getStops().get(index));
            }
    
        else
            for(int index = 0 ; index <= primaryStationIndex; index++){
                newLine.getStops().add(line.getStops().get(index));
            }

        
        for(Stop stop : newLine.getStops()){
            stop.setLine(newLine);
            stop.getStation().getStops().add(stop);
        }

        return newLine;

    }

    private static void removeAllLines(List<Line> lines , ModelData model){
        for(Line line: lines){

            for(Stop stop: line.getStops()){
                stop.getStation().getStops().remove(stop);
            }
            model.lines.remove(line);
        }
    }

    private static Line createLineWhenNoTerminal(ModelData model, Line line, boolean reversed,int primaryStationIndex ,int secondaryStationIndex ,boolean creatingFirstLine ){
        int lineID = generateID(model);
        String newLineName;
        
        if(creatingFirstLine){
            if (reversed)  newLineName = line.getName() + "-2";
            else newLineName = line.getName() + "-1";
        }

        else{
            if (reversed)  newLineName = line.getName() + "-1";
            else newLineName = line.getName() + "-2";
        }

        Line newLine = new Line(lineID, newLineName,line.getColor(),false,new ArrayList<>());

        if(creatingFirstLine){
            for(int index = 0 ; index <= primaryStationIndex; index++){
                newLine.getStops().add(line.getStops().get(index));
            }
        }

        else{
            for(int index = secondaryStationIndex ; index < line.getStops().size(); index++ ){
                newLine.getStops().add(line.getStops().get(index));
            }
        }
        

        for(Stop stop : newLine.getStops()){
            stop.setLine(newLine);
            stop.getStation().getStops().add(stop);
        }

        return newLine;

    }

    
    private static Line createReplacementLine(ModelData model, List<Line> lines, Line line ,int primaryStationIndex ,int secondaryStationIndex ,boolean oneStationIsTerminal,boolean primarySectionBoundaryTerminal){
        //generating line name.
        

        int lineID = generateID(model);
        //more than one line is selected.
        String newLineName = generateLineName(model, lines, line);

        Line replacementLine = new Line(lineID, newLineName, COLOR_STRING, false , new ArrayList<>());


        if(!oneStationIsTerminal || !primarySectionBoundaryTerminal){
            Stop firstStop = new Stop(line.getStops().get(primaryStationIndex).getStation(), replacementLine);
            firstStop.setLocation(line.getStops().get(primaryStationIndex).getLocation());
            replacementLine.getStops().add(firstStop);
        }
        else{
            replacementLine.getStops().add(line.getStops().get(primaryStationIndex));
            line.getStops().get(primaryStationIndex).setLine(replacementLine);
        }
        
        
        
        for(int stopIndex = (primaryStationIndex + 1) ; stopIndex < secondaryStationIndex ; stopIndex++ ){
            line.getStops().get(stopIndex).setLine(replacementLine);
            replacementLine.getStops().add(line.getStops().get(stopIndex));
        }

        if(!oneStationIsTerminal || primarySectionBoundaryTerminal){
            Stop lastStop = new Stop(line.getStops().get(secondaryStationIndex).getStation(),replacementLine);
            lastStop.setLocation(line.getStops().get(secondaryStationIndex).getLocation());
            replacementLine.getStops().add(lastStop);
        }

        else{
            replacementLine.getStops().add(line.getStops().get(secondaryStationIndex));
            line.getStops().get(secondaryStationIndex).setLine(replacementLine);
        }

        
        for(Stop stop : replacementLine.getStops()){
            stop.getStation().getStops().add(stop);
        }

        return  replacementLine;

    }

    private static boolean areConsecutiveStations(List<Station> arrangedStations, List<Station> stations){
        Station check;
        for(Station station : arrangedStations){
            check = MapModelUtil.findStation(stations, station.getName());
            if (check == null) return false;
        }
        return true;
    }

    private static List<Station> arrangeStations(List<Station> stations,  List<Line> lines){
        if( stations.isEmpty() || lines.isEmpty() || stations.size() < 2 || !allStationsInLine(lines, stations) )return new ArrayList<>();
        List<Station> arrangedStations = new ArrayList<>();
        int index = lines.get(0).getStops().size() -1;
        
        //finding the smallest select stop index
        for(Station station : stations){
            int currentIndex = MapModelUtil.findStop(lines.get(0).getStops(), station.getName());
            if(currentIndex < index) index = currentIndex;
        }

        for(int stopIndex = index,iterations = 0; iterations < stations.size(); stopIndex++,iterations++){
            arrangedStations.add(lines.get(0).getStops().get(stopIndex).getStation());

        }


        return arrangedStations;
    }

    private static boolean checkIfTerminal(Line line, Station station){

        return line.getStops().get(0).getStation().equals(station) ||
        line.getStops().get(line.getStops().size()-1).getStation().equals(station);


    }

    private static boolean oneStationIsTerminal(Line line, List<Station> arrangedStations){
        return line.getStops().get(0).getStation().equals(arrangedStations.get(0))
                ||line.getStops().get(0).getStation().equals(arrangedStations.get(arrangedStations.size()-1))
                ||line.getStops().get(line.getStops().size()-1).getStation().equals(arrangedStations.get(0))
                ||line.getStops().get(line.getStops().size()-1).getStation().equals(arrangedStations.get(arrangedStations.size()-1));
    }

    private static int generateID(ModelData model){
        int maxId = 0 ;
        for(Line line: model.lines ){
            if(line.getId() > maxId)
                maxId = line.getId();
        }
        return maxId + 1;
    }

    
    private static String generateLineName(ModelData model , List<Line> lines, Line line){
        if(lines.size() > 1){
            int count = 0;
            for (Line loopLine : model.lines){
                if(loopLine.getName().startsWith("P"))count++;
            }
            return ("P-" + (count + 1));

        }
        else{
            return ("P" + line.getName());
        }
    }


    private static boolean stationReversed(List<Station> stations, List<Station> arrangedStations){
        int firstStationSelectedIndex = -1;
        int lastStationSelectedIndex = -1;
        for (int i = 0; i < stations.size() ; i++){
            if(stations.get(i).equals(arrangedStations.get(0))) firstStationSelectedIndex = i;
            else if(stations.get(i).equals(arrangedStations.get(arrangedStations.size()-1)))lastStationSelectedIndex = i;
            
        }
        return firstStationSelectedIndex > lastStationSelectedIndex ;
    }

    private static boolean allStationsInLine(List<Line> lines, List<Station> stations){
        for(Line line: lines){
            for(Station station: stations){
                int stopIndex = MapModelUtil.findStop(line.getStops(), station.getName());
                if(stopIndex == -1)
                    return false;
            }
        }
        return true;
    }

    private static boolean areTerminalStations(List<Station> stations, List<Line> lines){
        List<Stop> lineStops;
        for(Line line : lines){
            lineStops = line.getStops();
            if(lineStops.get(0).getStation().equals(stations.get(0)) &&  lineStops.get(line.getStops().size()-1).getStation().equals(stations.get(stations.size()-1))
                || lineStops.get(0).getStation().equals(stations.get(stations.size()-1)) &&  lineStops.get(line.getStops().size()-1).getStation().equals(stations.get(0))){
                return true;
            }
        }
        return false;
    }


    public static void createAlternativeService(ModelData model, Station stationA, Station stationB) {
        // Ensure two distinct stations are selected
        if (stationA == null || stationB == null || stationA.equals(stationB) ) {
            return; // No operation performed
        }

        // Determine the next replacement line name based on existing lines
        int replacementLineCount = (int) model.lines.stream()
                .filter(line -> line.getName().startsWith("P"))
                .count();
        String newLineName = "P-" + (replacementLineCount + 1);

        List<Stop> stops = new ArrayList<>();
        
        // Create stops for stationA and stationB on this new line
        Stop stopA = new Stop(stationA, null); // Line will be set after creating the line
        Stop stopB = new Stop(stationB, null); // Line will be set after creating the line
        
        //adding the coordinates
        stopA.setLocation(stationA.getLocation());
        stopB.setLocation(stationB.getLocation());
        
        stops.add(stopA);
        stops.add(stopB);

        int lineId = generateID(model) ;

        // Create the new replacement line
        Line replacementLine = new Line(lineId , newLineName, COLOR_STRING, false , stops);

        // Link the stops to the line
        stopA.setLine(replacementLine);
        stopB.setLine(replacementLine);

        // Add the new line to the model
        model.lines.add(replacementLine);

        // Update the stations to include these stops
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
    }
}
