
package ENSDF_data;

import java.util.Comparator;
import java.util.Optional;

/**
 * This class represents energies in ENSDF records. Specifically, it is used
 * to parse the 'E' field in Level Records and Gamma Records (see ENSDF manual).
 * The <code>nonnumericPart</code> member is to handle the '+X', '+Y', etc. which
 * appear in high-spin datasets.
 * 
 * Date Modified: 19/11/2015
 * 
 * @author Michael Birch
 */
public class ENSDFEnergy {
    private String numericPart;
    private String nonnumericPart;
    private double numericPart_dbl;
    
    /**
     * Constructs an <code>ENSDFEnergy</code> object by splitting the given
     * String into its numeric and non-numeric parts.
     * @param E energy string
     */
    public ENSDFEnergy(String E){
        String[] Esplit;
        double tmp;
        Optional<Double> attempt;
        
        String trimmed = E.trim();
        attempt = DoubleUtils.safeParse(trimmed);
        if (attempt.isPresent()) {
            numericPart = trimmed;
            nonnumericPart = "";
            numericPart_dbl = attempt.get();
        } else {
            if (E.contains("+")) {
                Esplit = E.split("\\+");
                trimmed = Esplit[0].trim();
                attempt = DoubleUtils.safeParse(trimmed);
                if (attempt.isPresent()) {
                    numericPart = trimmed;
                    nonnumericPart = Esplit[1].trim();
                    numericPart_dbl = attempt.get();
                } else {
                    trimmed = Esplit[1].trim();
                    attempt = DoubleUtils.safeParse(trimmed);
                    if (attempt.isPresent()) {
                        numericPart = trimmed;
                        nonnumericPart = Esplit[0].trim();
                        numericPart_dbl = attempt.get();
                    } else {
                        numericPart = "0.0";
                        nonnumericPart = E.trim();
                        numericPart_dbl = 0d;
                    }
                }
            } else {
                numericPart = "0.0";
                nonnumericPart = E.trim();
                numericPart_dbl = 0d;
            }
        }
    }
    
    /**
     * Constructs an <code>ENSDFEnergy</code> object from the explicitly
     * given numeric and non-numeric parts.
     * @param n the numeric part
     * @param nn the non-numeric part
     */
    public ENSDFEnergy(String n, String nn){
        this.numericPart = n;
        this.nonnumericPart = nn;
        numericPart_dbl = Double.parseDouble(numericPart);
    }
    
    public ENSDFEnergy(double x, String n, String nn){
        this.numericPart = n;
        this.nonnumericPart = nn;
        this.numericPart_dbl = x;
    }
    
    /**
     * Returns the non-numeric part of the energy.
     * @return the non-numeric part of the energy
     */
    public String getNonNumericPart(){
        return this.nonnumericPart;
    }
    
    /**
     * Returns the numeric part of the energy.
     * @return the numeric part of the energy
     */
    public String getNumericPart(){
        return this.numericPart;
    }
    
    /**
     * Set the <code>numericPart</code> attribute.
     * @param d new <code>numericPart</code> value
     */
    public void setNumericPart(double d){
        this.numericPart = String.valueOf(d);
        this.numericPart_dbl = d;
    }
    
    /**
     * Returns the difference between this energy and the given energy. The 
     * result keeps the non-numeric part from this energy.
     * @param E the energy to subtract
     * @return the difference between this energy and the given energy
     */
    public ENSDFEnergy diff(ENSDFEnergy E){
        double d;
        d = this.numericPart_dbl - E.numericPart_dbl;
        return new ENSDFEnergy(String.valueOf(d), this.nonnumericPart);
    }
    
    /**
     * Returns the difference between this energy and the given energy.
     * @param E the energy to subtract
     * @return the difference between this energy and the given energy
     */
    public double doubleDiff(ENSDFEnergy E){
        return this.numericPart_dbl - E.numericPart_dbl;
    }
    
    /**
     * Returns the arithmetic mean of the numeric parts of the given energies.
     * The first non-blank non-numeric part is taken for the mean, if any.
     * @param energies input to take the mean of
     * @return the arithmetic mean of the numeric parts of the given energies.
     */
    public static ENSDFEnergy mean(ENSDFEnergy[] energies){
        double total;
        int i;
        String nn;
        
        total = 0d;
        nn = "";
        for(i=0; i<energies.length; i++){
            if(nn.equals("") && !energies[i].nonnumericPart.equals("")){
                nn = energies[i].nonnumericPart;
            }
            total += energies[i].numericPart_dbl;
        }
        total /= (double)energies.length;
        
        return new ENSDFEnergy(String.valueOf(total), nn);
    }
    
    /**
     * Returns <code>true</code> if this energy matches the given energy. A match
     * is defined as the two non-numeric parts being identical and the difference
     * in their numeric parts being less than 3 keV.
     * @param E
     * @return <code>true</code> if this energy matches the given energy,
     * <code>false</code> otherwise.
     */
    public boolean isSame(ENSDFEnergy E){
        return this.nonnumericPart.equals(E.nonnumericPart) &&
                Math.abs(this.numericPart_dbl - E.numericPart_dbl) < 3d;
    }
    
    /**
     * Returns <code>true</code> if the non-numeric parts of this energy and
     * the given energy are the same.
     * @param E the given energy
     * @return <code>true</code> if the non-numeric parts of this energy and
     * the given energy are the same.
     */
    public boolean isNonnumericMatch(ENSDFEnergy E){
        return this.nonnumericPart.equals(E.nonnumericPart);
    }
    
    /**
     * Returns the energy as a string of the form "&lt; nonnumericPart &gt; + 
     * &lt; numericPart &gt;".
     * @return Returns the energy as a string of the form "&lt; nonnumericPart &gt; + 
     * &lt; numericPart &gt;".
     */
    @Override public String toString(){
        if(this.nonnumericPart.equals("")){
            return this.numericPart;
        }else{
            return this.numericPart + "+" + this.nonnumericPart;
        }
    }
    
    /**
     * Returns the numeric part of the energy as a double.
     * @return the numeric part of the energy as a double.
     */
    public double toDouble(){
        return this.numericPart_dbl;
    }
    
    /**
     * Comparator for sorting <code>ENSDFEnergy</code> objects.
     */
    public static Comparator<ENSDFEnergy> ENSDFEnergyComparator = new Comparator<ENSDFEnergy>(){
        @Override
        public int compare(ENSDFEnergy E1, ENSDFEnergy E2){
            Double E1n, E2n;
            
            //if non-numeric parts are identical then compare numeric
            //parts
            if(E1.nonnumericPart.equals(E2.nonnumericPart)){
                E1n = E1.numericPart_dbl;
                E2n = E2.numericPart_dbl;
                return E1n.compareTo(E2n);
            }else{
                return E1.nonnumericPart.compareTo(E2.nonnumericPart);
            }
        }
    };
}
