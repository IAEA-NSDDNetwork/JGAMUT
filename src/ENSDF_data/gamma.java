
package ENSDF_data;

import ensdf_datapoint.*;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class defines the data structure based on ENSDF gamma ray records.
 * 
 * Date Modified: 15/12/2014
 * 
 * @author Michael Birch
 */
public class gamma {
    public static final char[] intensitySymbols = {'*', '&', '@'};
    private ENSDFGammaRecord gammaRecord;
    private List<ENSDFGammaContinuationRecord> gammaContinuationRecords;
    private String intensitySymbol;
    private level parentLevel;
    private level finalLevel;
    private String Ig_renorm, DIg_renorm;
    private String source; //the ENSDF dataset this gamma ray comes from
    public Optional<Double> eg, deg, ig, dig;
    public String energyAveragingMethod; //name of the averaging method used to obtain Eg; if any
    public String energyChiSq; //chi^2 for energy (if obtained from an average)
    public String intensityAveragingMethod; //name of the averaging method used to obtain Ig; if any
    public String intensityChiSq; //chi^2 for intensity (if obtained from an average)
    public boolean matched; //used when determining which gammas are the same
    
    /**
     * Creates a gamma object from the ENSDF gamma record.
     * @param gr given ENSDF gamma record
     * @param s title of the dataset which the record comes from (i.e. the
     * source of the gamma-ray)
     */
    public gamma(ENSDFGammaRecord gr, String s){
        this.eg = DoubleUtils.safeParse(gr.getE());
        this.deg = DoubleUtils.safeParse(gr.getDE());
        this.ig = DoubleUtils.safeParse(gr.getRI());
        this.dig = DoubleUtils.safeParse(gr.getDRI());
        this.gammaRecord = gr;
        this.gammaContinuationRecords = new ArrayList<>();
        this.parentLevel = null;
        this.finalLevel = null;
        this.source = s;
        this.Ig_renorm = null;
        this.DIg_renorm = null;
        this.energyAveragingMethod = null;
        this.energyChiSq = null;
        this.intensityAveragingMethod = null;
        this.intensityChiSq = null;
        matched = false;
        
        this.intensitySymbol = "";
        if(!gr.getC().equals("")){
            for(int i=0; i<intensitySymbols.length; i++){
                if(gr.getC().charAt(0) == intensitySymbols[i]){
                    this.intensitySymbol = gr.getC();
                }
            }
        }
    }
    
    /**
     * Creates a gamma-ray given the 'E', 'DE', 'RI', 'DRI', 'M', 'MR' and
     * 'Q' fields of the gamma record (see ENSDF manual)
     * @param energy the gamma-ray energy
     * @param Denergy the uncertainty in the gamma-ray energy (ENSDF format)
     * @param intensity the relative intensity of the gamma-ray
     * @param Dintensity the uncertainty in the relative intensity (ENSDF format)
     * @param exu <code>true</code> if the gamma is expected, but unobserved
     * @param plu <code>true</code> if the gamma placement in the level scheme
     * is uncertain
     * @param s the title of the dataset the gamma-ray comes from (i.e. the source)
     * @param m the multipolarity of the gamma-ray
     * @param mr  the mixing ratio of the gamma-ray
     */
    public gamma(String energy, String Denergy, String intensity, 
            String Dintensity, boolean exu, boolean plu, String s, String m,
            String mr){
        this.eg = DoubleUtils.safeParse(energy);
        this.deg = DoubleUtils.safeParse(Denergy);
        this.ig = DoubleUtils.safeParse(intensity);
        this.dig = DoubleUtils.safeParse(Dintensity);
        this.gammaRecord = new ENSDFGammaRecord("NNNNN  G ");
        this.gammaRecord.setE(energy);
        this.gammaRecord.setDE(Denergy);
        this.gammaRecord.setRI(intensity);
        this.gammaRecord.setDRI(Dintensity);
        if(exu){
            this.gammaRecord.setQ("S");
        }
        if(plu){
            this.gammaRecord.setQ("?");
        }
        this.gammaRecord.setM(m);
        this.gammaRecord.setMR(mr);
        this.gammaContinuationRecords = new ArrayList<>();
        this.parentLevel = null;
        this.finalLevel = null;
        this.source = s;
        this.Ig_renorm = null;
        this.DIg_renorm = null;
        this.energyAveragingMethod = null;
        this.energyChiSq = null;
        this.intensityAveragingMethod = null;
        this.intensityChiSq = null;
        matched = false;
    }
    
    //Copy constructor
    /**
     * Copy constructor.
     * @param g the gamma to copy
     */
    public gamma(gamma g){
        this.eg = DoubleUtils.copy(g.eg);
        this.deg = DoubleUtils.copy(g.deg);
        this.ig = DoubleUtils.copy(g.ig);
        this.dig = DoubleUtils.copy(g.dig);
        this.gammaRecord = new ENSDFGammaRecord(g.getGammaRecord());
        if(g.parentLevel == null){
            this.parentLevel = null;
        }else{
            this.setParent(g.parentLevel);
        }
        if(g.finalLevel == null){
            this.finalLevel = null;
        }else{
            this.setFinalLevel(g.finalLevel);
        }
        this.source = g.source;
        this.Ig_renorm = g.Ig_renorm;
        this.DIg_renorm = g.DIg_renorm;
        this.energyAveragingMethod = g.energyAveragingMethod;
        this.energyChiSq = g.energyChiSq;
        this.intensityAveragingMethod = g.intensityAveragingMethod;
        this.intensityChiSq = g.intensityChiSq;
        this.intensitySymbol = g.intensitySymbol;
        this.gammaContinuationRecords = new ArrayList<>(g.gammaContinuationRecords);
        this.matched = g.matched;
    }
    
    /**
     * Returns the ENSDF gamma record associated with this gamma object.
     * @return the ENSDF gamma record associated with this gamma object.
     */
    public ENSDFGammaRecord getGammaRecord(){
        return this.gammaRecord;
    }
    
    /**
     * Returns the title of the dataset this gamma-ray comes from.
     * @return the title of the dataset this gamma-ray comes from.
     */
    public String getSource(){
        return this.source;
    }
    
    /**
     * Returns the energy of the gamma-ray
     * @return the energy of the gamma-ray
     */
    public ENSDFEnergy getEnergy(){
        if (this.eg.isPresent()) {
            return new ENSDFEnergy(eg.get(), this.gammaRecord.getE(), "");
        } else {
            return new ENSDFEnergy(0d,"", this.gammaRecord.getE());
        }
    }
    
    public double recoilCorrection() {
        double Eg = this.eg.orElse(0d);
        int massNumber = this.gammaRecord.getMassNumber();
        if (massNumber == 0) {
            return 0.0d;
        } else {
            return (5.3677E-7) * Eg * Eg / (double) massNumber;
        }
    }
    
    /**
     * Returns the energy of the gamma-ray in the center of mass frame.
     * (I.e. after recoil correction)
     * @return the energy of the gamma-ray in the center of mass frame
     */
    public ENSDFEnergy getCMEnergy(){
        ENSDFEnergy e = this.getEnergy();
        e.setNumericPart(e.toDouble() + recoilCorrection());
        return e;
    }
    
    /**
     * Returns the String representing the energy of the gamma-ray, i.e. the
     * contents of the 'E' field in the ENSDF gamma record.
     * @return the String representing the energy of the gamma-ray, i.e. the
     * contents of the 'E' field in the ENSDF gamma record
     */
    public String getEg(){
        return this.gammaRecord.getE();
    }
    /**
     * Returns the uncertainty in the energy of the gamma-ray in ENSDF format.
     * @return the uncertainty in the energy of the gamma-ray in ENSDF format.
     */
    public String getDEg(){
        return this.gammaRecord.getDE();
    }
    
    /**
     * Returns the symbol which gives notes about how the intensity of this gamma
     * was treated (eg intensity split or gamma multiply placed). See the ENSDF
     * manual for details.
     * @return the symbol which gives notes about how the intensity of this gamma
     * was treated (eg intensity split or gamma multiply placed). See the ENSDF
     * manual for details.
     */
    public String getIntensitySymbol(){
        if(this.intensitySymbol == null){
            this.intensitySymbol = "";
        }
        return this.intensitySymbol;
    }
    
    /**
     * Returns the relative intensity of the gamma-ray.
     * @param renorm if <code>true</code> then the intensity is normalized such
     * that the strongest gamma from the parent level is 100. Otherwise, the
     * quantity returned is the same as in the dataset from which the gamma
     * was taken.
     * @return the relative intensity of the gamma-ray
     */
    public String getIg(boolean renorm){
        if(renorm){
            if (this.Ig_renorm != null) {
                return this.Ig_renorm;
            } else {
                return "";
            }
        }else{
            return this.gammaRecord.getRI();
        }
    }
    /**
     * Returns the uncertainty of the relative intensity of the gamma-ray,
     * in the ENSDF format.
     * @param renorm if <code>true</code> then the uncertainty is multiplied
     * by the same constant as the relative intensity itself in order to 
     * normalize the strongest gamma of the parent level to 100. Otherwise, the
     * quantity returned is the same as in the dataset from which the gamma
     * was taken.
     * @return the uncertainty of the relative intensity of the gamma-ray,
     * in the ENSDF format
     */
    public String getDIg(boolean renorm){
        if(renorm){
            if (this.DIg_renorm != null) {
                return this.DIg_renorm;
            } else {
                return "";
            }
        }else{
            return this.gammaRecord.getDRI();
        }
    }
    /**
     * Calls <code>{@link #getIg(boolean) getIg(false)}</code>
     * @return the relative intensity of the gamma-ray
     */
    public String getIg(){
        return this.getIg(false);
    }
    /**
     * Calls <code>{@link #getDIg(boolean) getDIg(false)}</code>
     * @return the uncertainty in the relative intensity of the gamma-ray
     */
    public String getDIg(){
        return this.getDIg(false);
    }
    
    /**
     * Returns the {@link ensdf_datapoint.dataPt dataPt} object created from
     * the relative intensity and its uncertainty
     * @return the {@link ensdf_datapoint.dataPt dataPt} object created from
     * the relative intensity and its uncertainty
     */
    public dataPt getIntensityDataPt(){
        dataPt result;
        result = new dataPt();
        if(dataPt.isParsable(getIg() + " " + getDIg(), result)){
            return result;
        }else{
            return null;
        }
    }

    /**
     * Returns the initial level of the gamma-ray.
     * @return the initial level of the gamma-ray.
     */
    public level getParent(){
        return this.parentLevel;
    }
    /**
     * Returns the final levels of the gamma-ray.
     * @return the final levels of the gamma-ray.
     */
    public level getFinalLevel(){
        return this.finalLevel;
    }
    /**
     * Returns <code>true</code> if the gamma-ray is expected, but unobserved
     * in the dataset from which it was taken. I.e. <code>true</code> if there
     * is an 'S' in the Q field of the gamma record (see ENSDF manual)
     * @return <code>true</code> if the gamma-ray is expected, but unobserved
     * in the dataset from which it was taken
     */
    public boolean isExpectedUnobserved(){
        return this.gammaRecord.getQ().equals("S");
    }
    /**
     * Returns <code>true</code> if the gamma-ray's placement in the level scheme
     * is uncertain in the dataset from which it was taken.
     * I.e. <code>true</code> if there
     * is a '?' in the Q field of the gamma record (see ENSDF manual)
     * @return <code>true</code> if the gamma-ray's placement in the level scheme
     * is uncertain in the dataset from which it was taken.
     */
    public boolean isPlacementUncertain(){
        return this.gammaRecord.getQ().equals("?");
    }
    
    /**
     * Returns <code>true</code> if the gamma was taken from the ADOPTED LEVELS,
     * GAMMAS dataset.
     * @return <code>true</code> if the gamma was taken from the ADOPTED LEVELS,
     * GAMMAS dataset.
     */
    public boolean isAdopted(){
        return this.source.equals("ADOPTED LEVELS, GAMMAS");
    }
    
    /**
     * Adds the information contained in the specified gamma continuation record
     * to this gamma object.
     * @param gcr given gamma continuation record
     */
    public void addGammaContinuationRecord(ENSDFGammaContinuationRecord gcr){
        this.gammaContinuationRecords.add(gcr);
        for(int i=0; i<intensitySymbols.length && this.intensitySymbol.equals("");
                i++){
            if(gcr.hasFLAG(intensitySymbols[i])){
                this.intensitySymbol = String.valueOf(intensitySymbols[i]);
            }
        }
    }
    
    /**
     * Sets the initial level of this gamma-ray to be the given level
     * @param p the given level
     */
    public final void setParent(level p){
        if(this.parentLevel != null){
            this.parentLevel.removeOutGamma(this);
        }
        this.parentLevel = p;
        if(p != null){
            p.addOutGamma(this);
        }
    }
    /**
     * Sets the final level of this gamma-ray to be the given level
     * @param f the given level
     */
    public final void setFinalLevel(level f){
        if(this.finalLevel != null){
            this.finalLevel.removeInGamma(this);
        }
        this.finalLevel = f;
        if(f != null){
            f.addInGamma(this);
        }
    }
    
    /**
     * Sets the intensity of the gamma to the given String. Note that
     * this changes the 'RI' field in the associated ENSDF gamma record.
     * @param intensity the new intensity string
     */
    public void setIg(String intensity){
        this.gammaRecord.setRI(intensity);
        this.ig = DoubleUtils.safeParse(intensity);
    }
    /**
     * Sets the uncertainty of the intensity of the gamma to the given String.
     * The uncertainty must be given in the ENSDF format. This method
     * directly changes the 'DRI' field of the associated ENSDF gamma record.
     * @param Dintensity the new intensity uncertainty string
     */
    public void setDIg(String Dintensity){
        this.gammaRecord.setDRI(Dintensity);
        this.dig = DoubleUtils.safeParse(Dintensity);
    }
    
    /**
     * Sets the intensity of the gamma-ray to the given numeric values of the
     * central value and uncertainty. Note this method also changes the
     * 'RI' and 'DRI' fields of the associated ENSDF gamma record appropriately.
     * @param I new intensity central value
     * @param DI new intensity uncertainty
     */
    public void setNumericIntensity(double I, double DI){
        String tmp = dataPt.ENSDFprint(String.valueOf(I), String.valueOf(DI), false);
        this.gammaRecord.setRI(tmp.split(" ")[0].trim());
        this.gammaRecord.setDRI(tmp.split(" ")[1].trim());
        this.ig = Optional.of(I);
        this.dig = Optional.of(DI);
    }
    
    /**
     * Sets the uncertainty of the intensity to be the given numeric value.
     * This method also changes the 'DRI' field of the associated ENSDF 
     * gamma record appropriately
     * @param Dintensity the new intensity uncertainty
     */
    public void setNumericDIg(double Dintensity){
        String tmp = dataPt.ENSDFprint(this.gammaRecord.getRI(), 
                String.valueOf(Dintensity), false);
        this.gammaRecord.setRI(tmp.split(" ")[0].trim());
        this.gammaRecord.setDRI(tmp.split(" ")[1].trim());
        this.dig = Optional.of(Dintensity);
    }
    
    /**
     * Sets the energy of the gamma-ray to the given String.
     * This method directly changes the 'E' field of the associated ENSDF gamma record.
     * @param energy the new energy
     */
    public void setEg(String energy) {
        this.gammaRecord.setE(energy);
        this.eg = DoubleUtils.safeParse(energy);
    }
    
    /**
     * Sets the uncertainty of the energy of the gamma-ray to the given String. 
     * This method directly changes the 'DE' field of the associated ENSDF gamma record.
     * @param Denergy the new energy uncertainty
     */
    public void setDEg(String Denergy){
        this.gammaRecord.setDE(Denergy);
        this.deg = DoubleUtils.safeParse(Denergy);
    }
    
    /**
     * Sets the energy of the gamma-ray to the numeric values of the given
     * central value and uncertainty. This method also changes the 'E' and 'DE'
     * fields of the associated ENSDF gamma record appropriately.
     * @param E the new energy central value
     * @param DE the new energy uncertainty
     */
    public void setNumericEnergy(double E, double DE){
        String tmp = dataPt.ENSDFprint(String.valueOf(E), String.valueOf(DE), false);
        this.gammaRecord.setE(tmp.split(" ")[0].trim());
        this.gammaRecord.setDE(tmp.split(" ")[1].trim());
        this.eg = Optional.of(E);
        this.deg = Optional.of(DE);
    }
    
    /**
     * Sets the uncertainty of the energy to the given numeric value.
     * This method also changes the 'DE' field of the associated ENSDF 
     * gamma record appropriately.
     * @param Denergy the new energy uncertainty
     */
    public void setNumericDEg(double Denergy){
        String tmp = dataPt.ENSDFprint(this.gammaRecord.getE(), 
                String.valueOf(Denergy), false);
        this.gammaRecord.setE(tmp.split(" ")[0].trim());
        this.gammaRecord.setDE(tmp.split(" ")[1].trim());
        this.deg = Optional.of(Denergy);
    }
    
    /**
     * Sets the value of the intensity of the gamma-ray to the given String,
     * which must be the value when the intensity is normalized such that the
     * strongest gamma from the parent level is 100.
     * @param intensity the new normalized intensity.
     */
    public void setIg_renorm(String intensity){
        this.Ig_renorm = intensity;
    }
    /**
     * Sets the uncertainty of the intensity of the gamma-ray to the given String,
     * which must be the value when the intensity is normalized such that the
     * strongest gamma from the parent level is 100. The uncertainty must be given
     * in the ENSDF format
     * @param Dintensity the new normalized intensity uncertainty.
     */
    public void setDIg_renorm(String Dintensity){
        this.DIg_renorm = Dintensity;
    }
    
    /**
     * Sets the source (title of the dataset the gamma comes from) to the
     * given String.
     * @param s the new gamma source
     */
    public void setSource(String s){
        this.source = s;
    }
    
    /**
     * Sets the intensity symbol (one of '@', '&', '*').
     * @param s the new symbol
     */
    public void setIntensitySymbol(String s){
        this.intensitySymbol = s;
    }
    
    /**
     * Returns <code>true</code> if the final level of the gamma-ray is specified
     * by an 'FL=' flag in the continuation record.
     * @return <code>true</code> if the final level of the gamma-ray is specified
     * by an 'FL=' flag in the continuation record.
     */
    public boolean hasFL(){
        if(this.gammaContinuationRecords.isEmpty()){
            return false;
        }
        for(ENSDFGammaContinuationRecord gcr : this.gammaContinuationRecords){
            if(gcr.hasFL()){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the energy of the final level specified by an 'FL=' flag in the
     * continuation record.
     * @return the energy of the final level specified by an 'FL=' flag in the
     * continuation record.
     */
    public String getFL(){
        if(this.gammaContinuationRecords.isEmpty()){
            return "";
        }
        for(ENSDFGammaContinuationRecord gcr : this.gammaContinuationRecords){
            if(gcr.hasFL()){
                return gcr.getFL();
            }
        }
        return "";
    }
    
    /**
     * Returns a String giving the energy of the gamma-ray.
     * @return a String giving the energy of the gamma-ray
     */
    @Override public String toString(){
        return "Gamma energy: " + this.getEg();
    }
    
    /**
     * Returns <code>true</code> if this gamma and the given gamma have identical
     * gamma records.
     * @param other the gamma to compare with
     * @return <code>true</code> if this gamma and the given gamma have identical
     * gamma records.
     */
    public boolean equals(gamma other){
        return this.gammaRecord.equals(other.gammaRecord);
    }
    /**
     * Returns <code>true</code> if this gamma has gamma record which is identical
     * to the given gamma record.
     * @param rec the gamma record to compare with
     * @return <code>true</code> if this gamma has gamma record which is identical
     * to the given gamma record.
     */
    public boolean equals(ENSDFGammaRecord rec){
        return this.gammaRecord.equals(rec);
    }
    /**
     * Returns <code>true</code> if this gamma has a gamma record which has the
     * same 'E', 'DE', 'RI', 'DRI', 'M', 'MR', and 'Q' fields as the given
     * gamma record.
     * @param rec the gamma record to compare with
     * @return <code>true</code> if this gamma has a gamma record which has the
     * same 'E', 'DE', 'RI', 'DRI', 'M', 'MR', and 'Q' fields as the given
     * gamma record.
     */
    public boolean partialEquals(ENSDFGammaRecord rec){
        return this.gammaRecord.partialEquals(rec);
    }
    
    /**
     * Returns <code>true</code> when the energy of this gamma ray is close 
     * to (within 3 keV) of the energy of the given gamma ray.
     * @param g gamma ray with which to compare energy
     * @return <code>true</code> when the energy of this gamma ray is close 
     * to (within 3 keV) of the energy of the given gamma ray.
     */
    public boolean energyMatch(gamma g){
        return this.getEnergy().isSame(g.getEnergy());
    }
    
    /**
     * Comparator used to sort the gamma-rays by energy
     */
    public static Comparator<gamma> energyComparator = new Comparator<gamma>(){
        @Override
        public int compare(gamma g1, gamma g2){
            return ENSDFEnergy.ENSDFEnergyComparator.compare(g1.getEnergy(), g2.getEnergy());
        }
    };
}
