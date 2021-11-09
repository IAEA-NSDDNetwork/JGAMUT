
package ENSDF_data;

import java.io.IOException;
import javax.swing.JOptionPane;
import text_io.*;
import java.util.List;
import java.util.ArrayList;


/**
 * This class provides methods for parsing ENSDF datasets.
 * 
 * Date Modified: 24/08/2015
 * 
 * @author Michael Birch
 */
public class ENSDFIO {
    
    /**
     * Reads the text file specified by <code>path</code> and parses the ENSDF
     * datasets contained within that file. Different datasets are separated by
     * an "end record" (blank line). If <code>saveLines</code> is <code>true</code>
     * then the Strings (lines of the text file) which were parsed to 
     * create the ENSDFDataset objects are also stored in memory.
     * @param path path to the text file containing the ENSDF datasets
     * @param decayData <code>true</code> if the data should be normalized like
     * a decay dataset (as opposed to an Adopted Levels, Gammas dataset
     * @param saveLines if <code>true</code>
     * then the Strings (lines of the text file) which were parsed to 
     * create the ENSDFDataset objects are also stored in memory.
     * @param verbose if <code>true</code> then this function will produce
     * message boxes for IO errors
     * @return List of ENSDFDataset objects containing the levels and gammas
     * parsed from the ENSDF input file.
     */
    public static final List<ENSDFDataset> readENSFile(String path, boolean decayData,
            boolean saveLines, boolean verbose){
        List<String> lines, completeFile;
        int i, count;
        List<ENSDFDataset> result;
        ENSDFDataset data;
        
        if(!textFileIO.exist(path)){
            if(verbose){
                JOptionPane.showMessageDialog(null, "Error! ENSDF input file not found.",
                        "ENSDF File I/O Error", JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
        
        try{
            completeFile = textFileIO.read(path);
        }catch(IOException e){
            JOptionPane.showMessageDialog(null, "I/O Error! " + e.getMessage(),
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        lines = new ArrayList<>(); //use as a buffer as the file is read
        result = new ArrayList<>();
        count = 0;
        for(i=0; i<completeFile.size(); i++){
            if(completeFile.get(i).trim().equals("")){ //end record is a blank line
                data = parseENSDF(lines, decayData);
                if(data == null){
                    return null;
                }else{
                    result.add(data); //parse the data set when an end record is found
                }
                if(saveLines){
                    result.get(count).setLines(new ArrayList<>(lines));
                }
                lines.clear();
                count += 1;
            }else{
                lines.add(completeFile.get(i));
            }
        }
        //make sure the last dataset is parsed as well
        if(!lines.isEmpty()){
            result.add(parseENSDF(lines, decayData));
            if(saveLines){
                result.get(count).setLines(new ArrayList<>(lines));
            }
        }
        return result;
    }
    /**
     * Calls <code>{@link #readENSFile(java.lang.String, boolean) readENSFile(path, false)}
     * </code>.
     * @param path path to the text file containing the ENSDF datasets
     * @param decayData <code>true</code> if the data should be normalized like
     * a decay dataset (as opposed to an Adopted Levels, Gammas dataset)
     * @return List of ENSDFDataset objects containing the levels and gammas
     * parsed from the ENSDF input file.
     */
    public static final List<ENSDFDataset> readENSFile(String path, boolean decayData){
        return readENSFile(path, decayData, false, true);
    }
    
    /**
     * Gives a message box informing the user that no Identification record was
     * found in the ENSDF file, indicating that the input is not in the 
     * correct ENSDF format.
     */
    private static void ENSDFidError(){
        JOptionPane.showMessageDialog(null, "ENSDF file format error! Identification record not found.",
                "ENSDF File Format Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Creates an ENSDFDataset object by parsing the lines provided in the 
     * ENSDF format.
     * @param lines lines of an input ENSDF dataset
     * @param decayData <code>true</code> if the data should be normalized like
     * a decay dataset (as opposed to an Adopted Levels, Gammas dataset)
     * @return ENSDFDataset object by parsing the lines provided in the 
     * ENSDF format
     */
    public static final ENSDFDataset parseENSDF(List<String> lines, boolean decayData){
        int i;
        String rType; //record type
        ENSDFLevelRecord lvlRecord;
        ENSDFGammaRecord gammaRecord;
        String title, refs, nucid; //data set identification
        level parentLevel;
        gamma previousGamma;
        ENSDFDataset result;
        
        result = null;
        title = "";
        nucid = ENSDFRecord.getCol(lines.get(0), 1, 5);
        for(i=0; i<lines.size(); i++){
            if(lines.get(i).trim().equals("")){
                continue; //skip blank lines
            }
            rType = ENSDFRecord.getCol(lines.get(i), 6, 8);
            if(rType.length() >= 3){
                if(rType.substring(1).equals(" G")){ //record is a gamma continuation record
                    rType = "2 G"; //handle all continuation records by making the
                                   //first arbitrary alphanumeric character equal to 2
                }
            }
            switch(rType) {
                case "": //Identification record
                    title = ENSDFRecord.getCol(lines.get(i), 10, 39);
                    refs = ENSDFRecord.getCol(lines.get(i), 40, 65);
                    result = new ENSDFDataset(nucid, title, refs);
                    break;
                case "L": //Level record
                    lvlRecord = new ENSDFLevelRecord(lines.get(i));
                    try{
                        result.addLevel(new level(lvlRecord, title), false);
                    }catch(NullPointerException e){
                        ENSDFidError();
                        return null;
                    }
                    break;
                case "G": //Gamma record
                    gammaRecord = new ENSDFGammaRecord(lines.get(i));
                    
                    try{
                        parentLevel = result.getLastLevel();
                        if(parentLevel == null){ //unplaced gamma rays
                            break; //skip unplaced gamma rays
                        }
                        result.addGamma(new gamma(gammaRecord, title), parentLevel);
                        
                    }catch(NullPointerException e){
                        ENSDFidError();
                        return null;
                    }
                    break;
                case "2 G": //Gamma continuation record
                    try{
                        if(result.getLastLevel() == null){ //continuation record for an unplaced gamma
                            break; //skip unplaced gamma rays
                        }
                    }catch(NullPointerException e){
                        ENSDFidError();
                        return null;
                    }
                    
                    try{
                        previousGamma = result.getLastGamma();
                        if(previousGamma == null){
                            JOptionPane.showMessageDialog(null, "ENSDF file format error! Gamma continuation record found before gamma record.", "ENSDF Format Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }catch(NullPointerException e){
                        ENSDFidError();
                        return null;
                    }
                    previousGamma.addGammaContinuationRecord(new ENSDFGammaContinuationRecord(lines.get(i)));
            }
        }
        
        try{
            result.findFinalLevels();
            result.renormalizeGammaRays(decayData);
        }catch(NullPointerException e){
            //Do nothing. If we somehow reached this point with result being
            //null without hitting one of the above try-catch clauses then
            //something is really wrong.
        }
        return result;
    }
}
