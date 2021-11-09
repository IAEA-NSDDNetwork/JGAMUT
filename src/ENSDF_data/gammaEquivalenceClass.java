
package ENSDF_data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import averagingAlgorithms.averagingMethods;
import averagingAlgorithms.averagingReport;
import ensdf_datapoint.dataPt;

/**
 * This class defines the methods used to decide if gammas from different
 * datasets are equivalent (ie the same gamma observed in different 
 * experiments).
 * 
 * Date Modified: 06/07/2015
 * 
 * @author Michael Birch
 */
public class gammaEquivalenceClass {
    private List<gamma> gammas;
    private String levelString;
    
    /**
     * Create an empty equivalence class.
     */
    public gammaEquivalenceClass(){
        gammas = new ArrayList<>();
        this.levelString = "";
    }
    
    public void setLevelString(String s){
        this.levelString = s;
    }
    
    public String getLevelString(){
        return this.levelString;
    }
    
    public void clear(){
        this.gammas.clear();
    }
    
    /**
     * Returns <code>true</code> when no gammas are in the equivalence class.
     * @return <code>true</code> when no gammas are in the equivalence class.
     */
    public boolean isEmpty(){
        return this.gammas.isEmpty();
    }
    
    /**
     * Returns <code>true</code> if there is a gamma in the equivalence class
     * that is from the specified source.
     * @param s the specified source to look for.
     * @return <code>true</code> if there is a gamma in the equivalence class
     * that has the specified source
     */
    public boolean containsSource(String s){
        for(gamma g : gammas){
            if(g.getSource().equals(s)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns <code>true</code> when the given gamma belongs to the
     * equivalence class.
     * @param g the gamma to check belonging
     * @return <code>true</code> when the given gamma belongs to the
     * equivalence class.
     */
    public boolean belongs(gamma g){
        boolean result;
        
        if(this.gammas.isEmpty()){
            return true;
        }
        
        //two gammas from the same dataset cannont be equivalent unless
        //they are the same gamma
        for(gamma gam : gammas){
            if(gam.getSource().equals(g.getSource())){
                return false;
            }
        }
        
        result = true;
        //gamma rays which have the same parent and final levels must be
        //the same
        try{
            result = result && gammas.get(0).getParent().equals(g.getParent());
        }catch(NullPointerException e){
            result = result && (gammas.get(0).getParent() == g.getParent());
        }
        try{
            result = result && gammas.get(0).getFinalLevel().equals(g.getFinalLevel());
        }catch(NullPointerException e){
            result = result && (gammas.get(0).getFinalLevel() == g.getFinalLevel());
        }
        
        return result;
    }
    
    /**
     * Adds a gamma to the equivalence class.
     * @param g the gamma to add
     */
    public void add(gamma g){
        this.gammas.add(g);
    }
    
    /**
     * Returns the list of levels in the equivalence class.
     * @return the list of levels in the equivalence class.
     */
    public List<gamma> getGammas(){
        return this.gammas;
    }
    
    /**
     * Returns the gamma from the equivilance class with the specified
     * source, if any.
     * @param s the specified source
     * @return the gamma from the equivilance class with the specified
     * source, if any
     */
    public gamma getGammaBySource(String s){
        for(gamma g : gammas){
            if(g.getSource().equals(s)){
                return g;
            }
        }
        return null;
    }
    
    /**
     * Returns <code>true</code> if a gamma from 'ADOPTED LEVELS, GAMMAS'
     * is present in the equivalence class.
     * @return <code>true</code> if a gamma from 'ADOPTED LEVELS, GAMMAS'
     * is present in the equivalence class.
     */
    public final boolean containsAdopted(){
        for(gamma g : this.gammas){
            if(g.isAdopted() || g.getSource().contains("GAMUT")){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sorts the equivalence class by energy and moves the Adopted gamma to
     * the top, if present.
     */
    public void sort(){
        gamma adopted;
        
        Collections.sort(gammas, gamma.energyComparator);
        
        adopted = null;
        if(this.containsAdopted()){
            for(gamma g : this.gammas){
                if(g.isAdopted() || g.getSource().contains("GAMUT")){
                    adopted = g;
                    break;
                }
            }
            this.gammas.remove(adopted);
            this.gammas.add(0, adopted);
        }
    }
    
    /**
     * Set Ig and DIg to "" for gammas with non-empty
     * intensity symbol.
     */
    public void clearUnresolvedIntensities(){
        for(gamma g : this.gammas){
            if(!g.getIntensitySymbol().isEmpty()){
                g.setIg("");
                g.setDIg("");
                g.setIg_renorm("");
                g.setDIg_renorm("");
            }
        }
    }
    
    /**
     * Returns a gamma which has the average energy and intensity of the gammas
     * in the equivilance class.
     * @param averagingMethod the averaging method to use to take the average
     * @param rptE the {@link averagingAlgorithms.averagingReport averagingReport}
     * object to store the details of the energy average calculation
     * @param rptI the {@link averagingAlgorithms.averagingReport averagingReport}
     * object to store the details of the intensity average calculation
     * @param limitMinUncert <code>true</code> if the uncertainty in the result
     * cannot be lower than the lowest uncertainty of any of the measurements 
     * which contributed to the average.
     * @return a gamma which has the average energy and intensity of the gammas
     * in the equivilance class.
     */
    public gamma average(BiFunction<dataPt[], averagingReport, dataPt> averagingMethod, 
            averagingReport rptE, averagingReport rptI, boolean limitMinUncert){
        gamma result;
        List<dataPt> energies, intensities;
        String tmp, energyResult, intensityResult;
        dataPt averageEg, averageIg;
        double minUncertE, minUncertI;
        gamma tmpG;
        
        energies = new ArrayList<>();
        intensities = new ArrayList<>();
        
        for(gamma g : this.gammas){
            //Adopted levels do not contribute to average
            if(g.isAdopted()){
                continue;
            }
            tmp = g.getSource();
            tmp = tmp + ": " + g.getEg() + " " + g.getDEg();
            if(dataPt.isParsable(tmp)){
                energies.add(dataPt.constructFromString(tmp));
            }
            tmp = g.getSource() + ": " + g.getIg(true) + " " + g.getDIg(true);
            if(dataPt.isParsable(tmp)){
                intensities.add(dataPt.constructFromString(tmp));
            }
        }
        
        if(energies.isEmpty()){
            tmpG = uwtAverage(false, true);
            double deg = tmpG.deg.orElse(0d);
            averageEg = new dataPt(tmpG.getEnergy().toDouble(), deg, deg);
            
        }else if(energies.size() == 1){
            averageEg = energies.get(0);
        }else{
            averageEg = averagingMethod.apply(energies.toArray(new dataPt[0]), rptE);
        }
        if(intensities.isEmpty()){
            averageIg = null;
        }else if(intensities.size() == 1){
            averageIg = intensities.get(0);
        }else{
            averageIg = averagingMethod.apply(intensities.toArray(new dataPt[0]), rptI);
        }
        try{
            rptE.dataSetName = this.levelString + " -- Eg: " + 
                    String.valueOf(Math.round(averageEg.getValue()));
            rptI.dataSetName = this.levelString + " -- Ig: " + 
                    String.valueOf(Math.round(averageEg.getValue()));
        }catch(NullPointerException e){
            //do nothing, no reports
        }
        try{
            minUncertE = rptE.minUncert();
        }catch(NullPointerException e){
            minUncertE = 0d;
        }
        try{
            minUncertI = rptI.minUncert();
        }catch(NullPointerException e){
            minUncertI = 0d;
        }
        if(limitMinUncert){
            if(averageEg.getUpper() < minUncertE){
                averageEg.setUpper(minUncertE);
                averageEg.setLower(minUncertE);
            }
        }
        tmp = averageEg.toString(false).replace("(", " ").replace(")", "");
        energyResult = dataPt.ENSDFprint(tmp.split(" ")[0],  tmp.split(" ")[1], true);
        try{
            if(limitMinUncert){
                if(averageIg.getUpper() < minUncertI){
                   averageIg.setUpper(minUncertI);
                   averageIg.setLower(minUncertI);
                }
            }
            intensityResult = dataPt.ENSDFprint(String.valueOf(averageIg.getValue()), 
                String.valueOf(averageIg.getUpper()), false);
            result = new gamma(energyResult.split(" ")[0], energyResult.split(" ")[1],
                intensityResult.split(" ")[0], intensityResult.split(" ")[1], false,
                false, this.levelString + " average gamma: " + 
                    String.valueOf(Math.round(averageEg.getValue())), "", "");
            result.getGammaRecord().setNucid(this.gammas.get(0).getGammaRecord().getNucid());
            result.setParent(this.gammas.get(0).getParent());
            result.setFinalLevel(this.gammas.get(0).getFinalLevel());
            return result;
        }catch(NullPointerException e){
            result = new gamma(energyResult.split(" ")[0], energyResult.split(" ")[1],
            "", "", false, false, this.levelString + " average gamma: " + 
                    String.valueOf(Math.round(averageEg.getValue())), "", "");
            result.getGammaRecord().setNucid(this.gammas.get(0).getGammaRecord().getNucid());
            result.setParent(this.gammas.get(0).getParent());
            result.setFinalLevel(this.gammas.get(0).getFinalLevel());
            return result;
        }
    }
    
    //returns a gamma ray which has the unweighted average energy 
    //and intensity of the gammas in the data set. This function 
    //will also use data points which have no uncertainty listed
    /**
     * Returns a gamma which has energy and intensity given by the unweighted
     * averages of the energies and intensities of the gammas in the equivalence
     * class.
     * @param limitMinUncert <code>true</code> if the uncertainty in the result
     * cannot be lower than the lowest uncertainty of any of the measurements 
     * which contributed to the average.
     * @param useNonNumericUncert if <code>true</code> then measurements that
     * do not have a numeric uncertainty will still be included in the average.
     * They are excluded otherwise.
     * @return a gamma which has energy and intensity given by the unweighted
     * averages of the energies and intensities of the gammas in the equivalence
     * class.
     */
    public gamma uwtAverage(boolean limitMinUncert, boolean useNonNumericUncert){
        List<dataPt> energies, intensities;
        String tmp, energyResult, intensityResult;
        dataPt averageEg, averageIg;
        boolean noUncertE, noUncertI;
        gamma result;
        double minUncertE, minUncertI;
        averagingReport rptE,rptI;
        
        energies = new ArrayList<>();
        intensities = new ArrayList<>();
        noUncertE = true;
        noUncertI = true;
        rptE = new averagingReport();
        rptI = new averagingReport();
        for(gamma g : this.gammas){
            //Adopted levels do not contribute to average
            if(g.isAdopted()){
                continue;
            }
            tmp = g.getSource();
            tmp = tmp + ": " + g.getEg() + " " + g.getDEg();
            if(dataPt.isParsable(tmp)){
                energies.add(dataPt.constructFromString(tmp));
                noUncertE = false;
            }else if(g.eg.isPresent() && useNonNumericUncert){
                energies.add(new dataPt(g.eg.get(), 0d, 0d));
            }
            tmp = g.getSource() + ": " + g.getIg(true) + " " + g.getDIg(true);
            if(dataPt.isParsable(tmp)){
                intensities.add(dataPt.constructFromString(tmp));
                noUncertI = false;
            }else if(isNumericString(g.getIg(true)) && useNonNumericUncert){
                intensities.add(new dataPt(Double.parseDouble(g.getIg(true)),
                    0d, 0d));
            }
        }
        
        if(energies.size() == 1){
            averageEg = energies.get(0);
        }else{
            averageEg = averagingMethods.unweightedAverage(energies.toArray(new dataPt[0]), rptE);
        }
        if(intensities.isEmpty()){
            //intensities could be empty because none of the gamma rays have
            //an uncertainty, e.g. in the case that it is a single gamma ray
            //de-exciting the level. We can add all those cases.
            for(gamma g : this.gammas){
                if(isNumericString(g.getIg(true)) && g.getDIg(true).equals("")){
                    intensities.add(new dataPt(Double.parseDouble(g.getIg(true)),
                        0d, 0d));
                }
            }
            if(intensities.isEmpty()){
                averageIg = null;
            }else if(intensities.size() == 1){
                averageIg = intensities.get(0);
            }else{
                averageIg = averagingMethods.unweightedAverage(intensities.toArray(new dataPt[0]), rptI);
            }
        }else if(intensities.size() == 1){
            averageIg = intensities.get(0);
        }else{
            averageIg = averagingMethods.unweightedAverage(intensities.toArray(new dataPt[0]), rptI);
        }
        
        try{
            minUncertE = rptE.minUncert();
        }catch(NullPointerException e){
            minUncertE = 0d;
        }
        try{
            minUncertI = rptI.minUncert();
        }catch(NullPointerException e){
            minUncertI = 0d;
        }
        if(limitMinUncert){
            if(averageEg.getUpper() < minUncertE){
                averageEg.setUpper(minUncertE);
                averageEg.setLower(minUncertE);
            }
        }
        energyResult = dataPt.ENSDFprint(String.valueOf(averageEg.getValue()), 
                String.valueOf(averageEg.getUpper()), false);
        try{
            if(limitMinUncert){
                if(averageIg.getUpper() < minUncertI){
                   averageIg.setUpper(minUncertI);
                   averageIg.setLower(minUncertI);
                }
            }
            intensityResult = dataPt.ENSDFprint(String.valueOf(averageIg.getValue()), 
                String.valueOf(averageIg.getUpper()), false);
            result = new gamma(energyResult.split(" ")[0], energyResult.split(" ")[1],
                intensityResult.split(" ")[0], intensityResult.split(" ")[1], false,
                false, this.levelString + " average gamma: " + 
                    String.valueOf(Math.round(averageEg.getValue())), "", "");
        }catch(NullPointerException e){
            result = new gamma(energyResult.split(" ")[0], energyResult.split(" ")[1],
            "", "", false, false, this.levelString + " average gamma: " + 
                    String.valueOf(Math.round(averageEg.getValue())), "", "");
        }
        
        if(noUncertE){
            result.setDEg("");
        }
        if(noUncertI){
            result.setDIg("");
        }
        
        result.getGammaRecord().setNucid(this.gammas.get(0).getGammaRecord().getNucid());
        result.setParent(this.gammas.get(0).getParent());
        result.setFinalLevel(this.gammas.get(0).getFinalLevel());
        
        return result;
    }
    
    /**
     * Returns a List of {@link ensdf_datapoint.dataPt dataPt} objects representing
     * the energies of all the gammas in the equivalence class.
     * @return a List of {@link ensdf_datapoint.dataPt dataPt} objects representing
     * the energies of all the gammas in the equivalence class.
     */
    public List<dataPt> getEnergyDataset(){
        List<dataPt> energies;
        String tmp;
        
        energies = new ArrayList<>();
        for(gamma g : this.gammas){
            //Adopted levels do not contribute to average
            if(g.isAdopted()){
                continue;
            }
            tmp = g.getSource();
            tmp = tmp + ": " + g.getEg() + " " + g.getDEg();
            if(dataPt.isParsable(tmp)){
                energies.add(dataPt.constructFromString(tmp));
            }
        }
        
        return energies;
    }
    
    /**
     * Returns a List of {@link ensdf_datapoint.dataPt dataPt} objects representing
     * the intensities of all the gammas in the equivalence class.
     * @return a List of {@link ensdf_datapoint.dataPt dataPt} objects representing
     * the intensities of all the gammas in the equivalence class.
     */
    public List<dataPt> getIntensityDataset(){
        List<dataPt> intensities;
        String tmp;
        
        intensities = new ArrayList<>();
        for(gamma g : this.gammas){
            //Adopted levels do not contribute to average
            if(g.isAdopted()){
                continue;
            }
            tmp = g.getSource() + ": " + g.getIg(true) + " " + g.getDIg(true);
            if(dataPt.isParsable(tmp)){
                intensities.add(dataPt.constructFromString(tmp));
            }
        }
        
        return intensities;
    }
    
    //returns a gamma ray which has the average energy and intensity of the 
    //gammas in the data set. This function begins with a weighted average,
    //tries NRM if chi^2 is too high and finally uses unweighted average
    //if chi^2 is still too high.
    /**
     * Returns a gamma which has the average energy and intensity of the gammas
     * in the equivalence class. This function begins with a weighted average,
     * tries NRM if the chi^2 is too high (higher than the critical chi^2 at
     * 99% confidence) and finally uses the unweighted average if the chi^2 is
     * still too high.
     * @param limitMinUncert <code>true</code> if the uncertainty in the result
     * cannot be lower than the lowest uncertainty of any of the measurements 
     * which contributed to the average.
     * @param useNonNumericUncert if <code>true</code> then measurements that
     * do not have a numeric uncertainty will still be included in the average.
     * They are excluded otherwise.
     * @return a gamma which has the average energy and intensity of the gammas
     * in the equivalence class
     */
    public gamma average(boolean limitMinUncert, boolean useNonNumericUncert){
        averagingReport rptE, rptI;
        gamma wtAve, NRM, uwtAve, result;
        boolean wtAcceptE, wtAcceptI, NRMAcceptE, NRMAcceptI;
        String Eg, DEg, Ig, DIg, EgAveMethod, IgAveMethod, EgChiSq, IgChiSq;
        List<dataPt> energies, intensities;
        
        //no need to do anything for a single gamma ray
        if(gammas.size() == 1){
            result = new gamma(gammas.get(0));
            result.setSource(this.levelString + " average gamma: " + 
                    gammas.get(0).getEg());
            //set intensity equal to normalized intensity since the intensity
            //is what gets used later, but the value we want it to have is
            //the normalized intensity
            result.setIg(result.getIg(true));
            result.setDIg(result.getDIg(true));
            result.energyAveragingMethod = "";
            result.energyChiSq = "";
            result.intensityAveragingMethod = "";
            result.intensityChiSq = "";
            return result;
        }
        
        averagingMethods.critChiSqConf = 0.99d; //99% confidence level
        rptE = new averagingReport();
        rptI = new averagingReport();
        
        energies = getEnergyDataset();
        intensities = getIntensityDataset();
        
        if(energies.size() > 1 || intensities.size() > 1){
            wtAve = average((dataPt[] dataset, averagingReport rpt) -> 
                        averagingMethods.weightedAverage(dataset, rpt), rptE, rptI, 
                        limitMinUncert);
            wtAcceptE = (rptE.reducedChiSq < rptE.criticalChiSq) && energies.size() > 1;
            wtAcceptI = (rptI.reducedChiSq < rptI.criticalChiSq) && intensities.size() > 1;
            EgChiSq = String.format("%1.3f", rptE.reducedChiSq);
            IgChiSq = String.format("%1.3f", rptI.reducedChiSq);
        }else{
            wtAve = null;
            wtAcceptE = false;
            wtAcceptI = false;
            EgChiSq = "-";
            IgChiSq = "-";
        }
            
        if(energies.size() > 2 || intensities.size() > 2){
            NRM = average((dataPt[] dataset, averagingReport rpt) -> 
                    averagingMethods.nrm(dataset, rpt), rptE, rptI, 
                    limitMinUncert);
            NRMAcceptE = (rptE.reducedChiSq < rptE.criticalChiSq) &&
                    energies.size() > 2;
            NRMAcceptI = (rptI.reducedChiSq < rptI.criticalChiSq) &&
                    intensities.size() > 2;
        }else{
            NRM = null;
            NRMAcceptE = false;
            NRMAcceptI = false;
        }
        
        uwtAve = uwtAverage(limitMinUncert, useNonNumericUncert);
        if(uwtAve.getEg().equals("NaN")){
            uwtAve = uwtAverage(limitMinUncert, true);
        }
        
        if(wtAcceptE){
            Eg = wtAve.getEg();
            DEg = wtAve.getDEg();
            EgAveMethod = "WtAve";
        }else if(NRMAcceptE){
            Eg = NRM.getEg();
            DEg = NRM.getDEg();
            EgAveMethod = "NRM";
            EgChiSq = String.format("%1.3f", rptE.reducedChiSq);
        }else if(energies.size() == 1){
            Eg = dataPt.ENSDFprint(String.valueOf(energies.get(0).getValue()),
                    String.valueOf(energies.get(0).getUpper()), false).split(" ")[0];
            DEg = dataPt.ENSDFprint(String.valueOf(energies.get(0).getValue()),
                    String.valueOf(energies.get(0).getUpper()), false).split(" ")[1];
            EgAveMethod = "";
            EgChiSq = "-";
        }else if(energies.isEmpty()){
            //there are no gamma rays that can be averaged 
            //most likely because none of the uncertainties are numeric
            Eg = uwtAve.getEg();
            DEg = "";
            EgAveMethod = "";
            EgChiSq = "-";
        }else{
            Eg = uwtAve.getEg();
            DEg = uwtAve.getDEg();
            EgAveMethod = "UnwtAve";
            EgChiSq = "-";
        }
        
        if(wtAcceptI){
            Ig = wtAve.getIg();
            DIg = wtAve.getDIg();
            IgAveMethod = "WtAve";
        }else if(NRMAcceptI){
            Ig = NRM.getIg();
            DIg = NRM.getDIg();
            IgAveMethod = "NRM";
            IgChiSq = String.format("%1.3f", rptI.reducedChiSq);
        }else if(intensities.size() == 1){
            Ig = dataPt.ENSDFprint(String.valueOf(intensities.get(0).getValue()),
                    String.valueOf(intensities.get(0).getUpper()), false).split(" ")[0];
            DIg = dataPt.ENSDFprint(String.valueOf(intensities.get(0).getValue()),
                    String.valueOf(intensities.get(0).getUpper()), false).split(" ")[1];
            IgAveMethod = "";
            IgChiSq = "-";
        }else if(intensities.isEmpty()){
            //there are no gamma rays that can be averaged 
            //most likely because none of the uncertainties are numeric
            Ig = uwtAve.getIg();
            DIg = "";
            IgAveMethod = "";
            IgChiSq = "-";
        }else{
            Ig = uwtAve.getIg();
            DIg = uwtAve.getDIg();
            IgAveMethod = "UnwtAve";
            IgChiSq = "-";
        }
        
        result = new gamma(Eg, DEg, Ig, DIg, false, false, 
                            this.levelString + " average gamma: " + Eg,
                            "", "");
        result.getGammaRecord().setNucid(this.gammas.get(0).getGammaRecord().getNucid());
        result.setParent(this.gammas.get(0).getParent());
        result.setFinalLevel(this.gammas.get(0).getFinalLevel());
        result.setIg_renorm(result.getIg());
        result.setDIg_renorm(result.getDIg());
        result.energyAveragingMethod = EgAveMethod;
        result.energyChiSq = EgChiSq;
        result.intensityAveragingMethod = IgAveMethod;
        result.intensityChiSq = IgChiSq;
        
        return result;
    }
    
    /**
     * Returns <code>true</code> if the given String can be parsed into a double.
     * @param s the given String
     * @return <code>true</code> if the given String can be parsed into a double
     */
    private static boolean isNumericString(String s){
        return DoubleUtils.isNumericString(s);
    }
}
