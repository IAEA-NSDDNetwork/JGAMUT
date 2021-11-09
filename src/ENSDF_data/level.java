
package ENSDF_data;

import ensdf_datapoint.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;

/**
 * This class defines the data structure based on ENSDF level records
 * 
 * Date Modified: 05/07/2015
 * 
 * @author Michael Birch
 */
public class level {
    private final ENSDFLevelRecord lvlRecord;
    private List<gamma> gRaysOut; //gamma rays for which this level is the parent
    private List<gamma> gRaysIn; //gamma rays for which this level is the final level
    private final String source; //the ENSDF dataset this gamma ray comes from
    private final String rawSpin, rawParity; //the spin and parity deduced from the J field of the ENSDF Level record
    private final ENSDFEnergy energy; //the energy of the level deduced from the E field of the ENSDF level record
    private final Optional<Double> de; //the uncertainty of the level energy from the DE field of the ENSDF level record
    public boolean matched; //used when determining which levels are the same
    public String[] gammaSources; //the datasets which measured a gamma depopulating this level
    public double[] beta; //the constants used in the GAMUT intensity algroithm
    
    /**
     * Constructs a level object from the given level record and source. The
     * gamma list is empty by default.
     * @param lr {@link ENSDFLevelRecord} object
     * @param s the title of the dataset the level record comes from
     */
    public level(ENSDFLevelRecord lr, String s){
        this.lvlRecord = lr;
        this.gRaysOut = new ArrayList<>();
        this.gRaysIn = new ArrayList<>();
        this.source = s;
        this.matched = false;
        this.rawParity = this.getParity();
        this.rawSpin = this.getSpin();
        this.energy = new ENSDFEnergy(lr.getE());
        this.gammaSources = null;
        this.beta = null;
        this.de = DoubleUtils.safeParse(lr.getDE());
    }
    
    /**
     * Copy constructor
     * @param l The <code>level</code> object to copy.
     */
    public level(level l){
        if (l.de.isPresent()) {
            this.de = Optional.of(l.de.get());
        } else {
            this.de = Optional.empty();
        }
        this.lvlRecord = new ENSDFLevelRecord(l.getLvlRecord());
        this.gRaysOut = new ArrayList<>(l.gRaysOut);
        this.gRaysIn = new ArrayList<>(l.gRaysIn);
        this.source = l.getSource();
        this.matched = l.matched;
        this.rawParity = l.rawParity;
        this.rawSpin = l.rawSpin;
        this.energy = new ENSDFEnergy(l.energy.getNumericPart(), l.energy.getNonNumericPart());
        if(l.gammaSources == null){
            this.gammaSources = null;
        }else{
            this.gammaSources = Arrays.copyOf(l.gammaSources, l.gammaSources.length);
        }
        if(l.beta == null){
            this.beta = null;
        }else{
            this.beta = Arrays.copyOf(l.beta, l.beta.length);
        }
    }
    
    /**
     * Adds a gamma to the list of gammas which decay from this level
     * @param g the <code>gamma</code> object to add
     */
    public void addOutGamma(gamma g){
        this.gRaysOut.add(g);
    }
    
    /**
     * Adds a gamma to the list of gammas which decay to this level
     * @param g the <code>gamma</code> object to add
     */
    public void addInGamma(gamma g){
        this.gRaysIn.add(g);
    }
    
    //getters
    /**
     * Returns the title of the dataset this level came from.
     * @return the title of the dataset this level came from.
     */
    public String getSource(){
        return this.source;
    }
    
    /**
     * Returns the {@link ENSDF_data.ENSDFLevelRecord ENSDF level record} object associated
     * with this level.
     * @return the {@link ENSDF_data.ENSDFLevelRecord ENSDF level record} object associated
     * with this level.
     */
    public ENSDFLevelRecord getLvlRecord(){
        return this.lvlRecord;
    }
    
    /**
     * Returns the energy of this level
     * @return the energy of this level
     */
    public final ENSDFEnergy getEnergy(){
        return this.energy;
    }
    
    /**
     * Returns the gamma-ray which depopulates this level with the specified
     * index
     * @param i the specified index
     * @return the gamma-ray which depopulates this level with the specified
     * index
     */
    public gamma getOutGRay(int i){
        return this.gRaysOut.get(i);
    }
    
    /**
     * Returns the gamma-ray which populates this level with the specified
     * index
     * @param i the specified index
     * @return the gamma-ray which populates this level with the specified
     * index
     */
    public gamma getInGRay(int i){
        return this.gRaysIn.get(i);
    }
    
    /**
     * Returns the List of gammas depopulating this level.
     * @return the List of gammas depopulating this level.
     */
    public List<gamma> getGammaOutList(){
        return this.gRaysOut;
    }
    
    /**
     * Returns the List of gammas populating this level.
     * @return the List of gammas populating this level.
     */
    public List<gamma> getGammaInList(){
        return this.gRaysIn;
    }
    
    /**
     * Returns an array of gammas depopulating this level.
     * @return an array of gammas depopulating this level.
     */
    public gamma[] getOutGRays(){
        return this.gRaysOut.toArray(new gamma[0]);
    }
    
    /**
     * Returns an array of gammas populating this level.
     * @return an array of gammas populating this level.
     */
    public gamma[] getInGRays(){
        return this.gRaysIn.toArray(new gamma[0]);
    }
    
    /**
     * Sets the energy of the level to the numeric values of the given
     * central value and uncertainty. This method also changes the 'E' and 'DE'
     * fields of the associated ENSDF level record appropriately.
     * @param E the new energy central value
     * @param DE the new energy uncertainty
     */
    public void setNumericEnergy(double E, double DE){
        String nonNumeric = this.getEnergy().getNonNumericPart();
        String energy = dataPt.ENSDFprint(String.valueOf(E), 
                String.valueOf(DE), false);
        if(nonNumeric.equals("")){
            this.lvlRecord.setE(energy.split(" ")[0].trim());
        }else{
            this.lvlRecord.setE(nonNumeric + "+" + energy.split(" ")[0].trim());
        }
        this.lvlRecord.setDE(energy.split(" ")[1].trim());
    }
    
    /**
     * Returns <code>true</code> if at least one gamma-ray either populating or
     * depopulating this level is not from the ADOPTED LEVELS, GAMMAS dataset
     * @return <code>true</code> if at least one gamma-ray either populating or
     * depopulating this level is not from the ADOPTED LEVELS, GAMMAS dataset
     */
    public boolean hasNonAdoptedGammas(){
        boolean result = (!this.gRaysIn.isEmpty()) || (!this.gRaysOut.isEmpty());
        int i;
        if(result){
            result = false;
            for(i=0; i<gRaysIn.size() && !result; i++){
                result = !gRaysIn.get(i).isAdopted();
            }
            for(i=0; i<gRaysOut.size() && !result; i++){
                result = !gRaysOut.get(i).isAdopted();
            }
        }
        return result;
    }
    
    /**
     * Returns <code>true</code> if this level comes from the ADOPTED LEVELS,
     * GAMMAS dataset.
     * @return <code>true</code> if this level comes from the ADOPTED LEVELS,
     * GAMMAS dataset.
     */
    public boolean isAdopted(){
        return this.source.equals("ADOPTED LEVELS, GAMMAS");
    }
    
    /**
     * Removes the specified gamma from the List of depopulating gammas.
     * @param g the specified gamma
     */
    public void removeOutGamma(gamma g){
        this.gRaysOut.remove(g);
    }
    /**
     * Removes the gamma with the specified index from the List of depopulating
     * gammas.
     * @param i the specified index
     */
    public void removeOutGamma(int i){
        this.gRaysOut.remove(i);
    }
    
    /**
     * Removes the specified gamma from the List of populating gammas.
     * @param g the specified gamma
     */
    public void removeInGamma(gamma g){
        this.gRaysIn.remove(g);
    }
    /**
     * Removes the gamma with the specified index from the List of populating
     * gammas.
     * @param i the specified index
     */
    public void removeInGamma(int i){
        this.gRaysIn.remove(i);
    }
    
    /**
     * Clears the <code>gRaysIn</code> and <code>gRaysOut</code> lists.
     */
    public void removeAllGammas(){
        this.gRaysIn.clear();
        this.gRaysOut.clear();
    }
    
    /**
     * Returns a String listing the level energy together with the energies
     * and intensities of all the depopulating gammas.
     * @return a String listing the level energy together with the energies
     * and intensities of all the depopulating gammas
     */
    @Override public String toString(){
        String result;
        int i;
        
        result = "Level energy: " + this.lvlRecord.getE() + "\n";
        result += "Gamma Rays: \n";
        for(i=0; i<this.gRaysOut.size(); i++){
            result += "\t " + this.gRaysOut.get(i).getEg() + "\t" +
                    this.gRaysOut.get(i).getIg() + "\n";
        }
        return result;
    }
    
    /**
     * Returns <code>true</code> when the energy of this level is close 
     * to (within 3 keV) of the energy of the given level.
     * @param l level with which to compare energy
     * @return <code>true</code> when the energy of this level is close 
     * to (within 3 keV) of the energy of the given level.
     */
    public boolean energyMatch(level l){
        return this.getEnergy().isSame(l.getEnergy());
    }
    
    /**
     * Returns the fraction of gamma rays which are similar in energy between
     * this level and the given level. Both populating and depopulating gammas
     * are considered.
     * @param l the level with which to compare gamma rays
     * @return fraction of gamma rays which are similar in energy between
     * this level and the given level.
     */
    public double gammaMatch(level l){
        int count, total;
        
        if(this.gRaysOut.isEmpty() && this.gRaysIn.isEmpty()){
            if(l.gRaysOut.isEmpty() && l.gRaysIn.isEmpty()){
                return 1d;
            }else{
                return 0;
            }
        }
        
        total = this.gRaysIn.size() + this.gRaysOut.size();
        count = 0;
        for(gamma g : this.gRaysOut){
            for(gamma g2: l.gRaysOut){
                if(g.energyMatch(g2)){
                    count += 1;
                    break;
                }
            }
        }
        for(gamma g : this.gRaysIn){
            for(gamma g2: l.gRaysIn){
                if(g.energyMatch(g2)){
                    count += 1;
                    break;
                }
            }
        }
        return (double)count/(double)total;
    }
    
    /**
     * Returns <code>true</code> when two levels are exactly equal (in the 
     * sense of their ENSDF Level records).
     * @param l level to compare to.
     * @return <code>true</code> when two levels are exactly equal (in the 
     * sense of their ENSDF Level records).
     */
    public boolean equals(level l){
        return this.getLvlRecord().equals(l.getLvlRecord());
    }
    
    /**
     * Returns the parity of the level if it is known, "" otherwise.
     * @return the parity of the level if it is known, "" otherwise
     */
    public final String getParity(){
        String J = this.lvlRecord.getJ();
        
        if(J.contains("+") && !J.contains("-")){
            return "+";
        }else if(!J.contains("+") && J.contains("-")){
            return "-";
        }else{
            return "";
        }
    }
    
    /**
     * Returns the spin of the level if it is known (multiple values possible
     * does not count as known) and "" otherwise.
     * @return Returns the spin of the level if it is known (multiple values possible
     * does not count as known) and "" otherwise
     */
    public final String getSpin(){
        String J = this.lvlRecord.getJ();
        
        //remove partity and tenitive assignment characters
        J = J.replace("+", "").replace("-", "").replace("(", "").replace(")", "");
        try{
            //return the remaining string if it is the only spin
            Integer.parseInt(J);
            return J;
        }catch(NumberFormatException e){
            //return nothing if multiple spins possible (e.g. J field
            //of the form (1, 2)+) because the spin is not 
            return "";
        }
    }
    
    /**
     * Discrete metric for Strings with the modification that the empty string
     * is equal to all strings.
     * @param s1 string to compare
     * @param s2 string to compare
     * @return 1000 if s1 != s2 and 0 if s1 = s2 or s1 = "" or s2 = ""
     */
    private static double stringDistance(String s1, String s2, boolean ignoreEmpty){
        if((s1.equals("") || s2.equals("")) && ignoreEmpty){ //ignore missing values
            return 0.0;
        }
        if(s1.equals(s2)){
            return 0.0d;
        }else{
            return 1000.0d;
        }
    }
    
    /**
     * Distance metric between levels used in the k-Medoids algorithm.
     * @param l1 level to compare
     * @param l2 level to compare
     * @return the distance between l1 and l2
     */
    public static double distance(level l1, level l2){
        double dE, de1, de2;
        
        if(l1.equals(l2) && l1.getSource().equals(l2.getSource())){
            return 0.0d;
        }
        
        String t1, t2;
        t1 = l1.getLvlRecord().getTAG();
        t2 = l2.getLvlRecord().getTAG();
        
        //if the tags are given they they alone determine equality
        if (!t1.isEmpty() && !t2.isEmpty()) {
            return stringDistance(t1, t2, false);
        }
        
        if (l1.de.isPresent() && l2.de.isPresent()) {
            de1 = l1.de.get();
            de2 = l2.de.get();
            dE = Math.sqrt(de1 * de1 + de2 * de2);
        } else {
            dE = 1d;
        }
        
        return stringDistance(l1.rawParity, l2.rawParity, true) + 
                stringDistance(l1.rawSpin, l2.rawSpin, true) + 
                Math.abs(l1.energy.doubleDiff(l2.energy))/dE + 
                stringDistance(l1.energy.getNonNumericPart(), l2.energy.getNonNumericPart(), false) + 
                (1000.0d - stringDistance(l1.source, l2.source, false));
    }
    
    /**
     * The Comparator for sorting levels by energy
     */
    public static Comparator<level> energyComparator = new Comparator<level>(){
        @Override
        public int compare(level l1, level l2){
            return ENSDFEnergy.ENSDFEnergyComparator.compare(l1.getEnergy(), l2.getEnergy());
        }
    };
}
