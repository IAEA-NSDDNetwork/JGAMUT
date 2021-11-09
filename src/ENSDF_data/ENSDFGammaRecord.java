
package ENSDF_data;

/**
 * This class implements the ENSDF Gamma Record (see ENSDF Manual).
 * 
 * Date Modified: 05/07/2015
 * 
 * @author Michael Birch
 */
public class ENSDFGammaRecord extends ENSDFRecord{
    private String nucid, E, DE, RI, DRI, M, MR, DMR, CC, 
            DCC, TI, DTI, C, COIN, Q;
    
    /**
     * Creates an <code>ENSDFGammaRecord</code> object from a Gamma record
     * in an ENS file according to the format defined in the ENSDF manual.
     * @param s ENSDF Gamma Record string
     */
    public ENSDFGammaRecord(String s){
        super(s);
        this.nucid = super.getNucid();
        this.E = ENSDFRecord.getCol(s, 10, 19);
        this.DE = ENSDFRecord.getCol(s, 20, 21);
        this.RI = ENSDFRecord.getCol(s, 22, 29);
        this.DRI = ENSDFRecord.getCol(s, 30, 31);
        this.M = ENSDFRecord.getCol(s, 32, 41);
        this.MR = ENSDFRecord.getCol(s, 42, 49);
        this.DMR = ENSDFRecord.getCol(s, 50, 55);
        this.CC = ENSDFRecord.getCol(s, 56, 62);
        this.DCC = ENSDFRecord.getCol(s, 63, 64);
        this.TI = ENSDFRecord.getCol(s, 65, 74);
        this.DTI = ENSDFRecord.getCol(s, 75, 76);
        this.C = ENSDFRecord.getCol(s, 77);
        this.COIN = ENSDFRecord.getCol(s, 78);
        this.Q = ENSDFRecord.getCol(s, 80);
    }
    
    /**
     * Copy constructor
     * @param gr <code>ENSDFGammaRecord</code> object to copy
     */
    public ENSDFGammaRecord(ENSDFGammaRecord gr){
        this(new String(gr.record));
    }
    
    @Override public String getRecordType(){
        return "G";
    }
    
    //getters
    /**
     * Returns the NUCID for the gamma (see ENSDF manual). Note that whitespace
     * is NOT trimmed.
     * @return the NUCID for the gamma (see ENSDF manual)
     */
    @Override public String getNucid(){
        return this.nucid;
    }
    /**
     * Returns the E field of the Gamma record (see ENSDF manual).
     * @return the E field of the Gamma record (see ENSDF manual).
     */
    public String getE(){
        if (this.E == null) {
            return "";
        } else {
            return this.E;
        }
    }
    /**
     * Returns the DE field of the Gamma record (see ENSDF manual).
     * @return the DE field of the Gamma record (see ENSDF manual).
     */
    public String getDE(){
        if (this.DE == null) {
            return "";
        } else {
            return this.DE;
        }
    }
    /**
     * Returns the RI field of the Gamma record (see ENSDF manual).
     * @return the RI field of the Gamma record (see ENSDF manual).
     */
    public String getRI(){
        return this.RI;
    }
    /**
     * Returns the DRI field of the Gamma record (see ENSDF manual).
     * @return the DRI field of the Gamma record (see ENSDF manual).
     */
    public String getDRI(){
        return this.DRI;
    }
    /**
     * Returns the M field of the Gamma record (see ENSDF manual).
     * @return the M field of the Gamma record (see ENSDF manual).
     */
    public String getM(){
        return this.M;
    }
    /**
     * Returns the MR field of the Gamma record (see ENSDF manual).
     * @return the MR field of the Gamma record (see ENSDF manual).
     */
    public String getMR(){
        return this.MR;
    }
    /**
     * Returns the DMR field of the Gamma record (see ENSDF manual).
     * @return the DMR field of the Gamma record (see ENSDF manual).
     */
    public String getDMR(){
        return this.DMR;
    }
    /**
     * Returns the CC field of the Gamma record (see ENSDF manual).
     * @return the CC field of the Gamma record (see ENSDF manual).
     */
    public String getCC(){
        return this.CC;
    }
    /**
     * Returns the DCC field of the Gamma record (see ENSDF manual).
     * @return the DCC field of the Gamma record (see ENSDF manual).
     */
    public String getDCC(){
        return this.DCC;
    }
    /**
     * Returns the TI field of the Gamma record (see ENSDF manual).
     * @return the TI field of the Gamma record (see ENSDF manual).
     */
    public String getTI(){
        return this.TI;
    }
    /**
     * Returns the DTI field of the Gamma record (see ENSDF manual).
     * @return the DTI field of the Gamma record (see ENSDF manual).
     */
    public String getDTI(){
        return this.DTI;
    }
    /**
     * Returns the C field of the Gamma record (see ENSDF manual).
     * @return the C field of the Gamma record (see ENSDF manual).
     */
    public String getC(){
        return this.C;
    }
    /**
     * Returns the COIN field of the Gamma record (see ENSDF manual).
     * @return the COIN field of the Gamma record (see ENSDF manual).
     */
    public String getCOIN(){
        return this.COIN;
    }
    /**
     * Returns the Q field of the Gamma record (see ENSDF manual).
     * @return the Q field of the Gamma record (see ENSDF manual).
     */
    public String getQ(){
        return this.Q;
    }
    
    //setters
    /**
     * Sets the NUCID of the Gamma record (see ENSDF manual).
     * @param s the NUCID of the Gamma record
     */
    public void setNucid(String s){
        this.nucid = s;
        setRecord();
    }
    /**
     * Sets the E field of the Gamma record (see ENSDF manual).
     * @param s the E field of the Gamma record (see ENSDF manual).
     */
    public void setE(String s){
        this.E = s;
        setRecord();
    }
    /**
     * Sets the DE field of the Gamma record (see ENSDF manual).
     * @param s the DE field of the Gamma record (see ENSDF manual).
     */
    public void setDE(String s){
        this.DE = s;
        setRecord();
    }
    /**
     * Sets the RI field of the Gamma record (see ENSDF manual).
     * @param s the RI field of the Gamma record (see ENSDF manual).
     */
    public void setRI(String s){
        this.RI = s;
        setRecord();
    }
    /**
     * Sets the DRI field of the Gamma record (see ENSDF manual).
     * @param s the DRI field of the Gamma record (see ENSDF manual).
     */
    public void setDRI(String s){
        this.DRI = s;
        setRecord();
    }
    /**
     * Sets the M field of the Gamma record (see ENSDF manual).
     * @param s the M field of the Gamma record (see ENSDF manual).
     */
    public void setM(String s){
        this.M = s;
        setRecord();
    }
    /**
     * Sets the MR field of the Gamma record (see ENSDF manual).
     * @param s the MR field of the Gamma record (see ENSDF manual).
     */
    public void setMR(String s){
        this.MR = s;
        setRecord();
    }
    /**
     * Sets the DMR field of the Gamma record (see ENSDF manual).
     * @param s the DMR field of the Gamma record (see ENSDF manual).
     */
    public void setDMR(String s){
        this.DMR = s;
        setRecord();
    }
    /**
     * Sets the CC field of the Gamma record (see ENSDF manual).
     * @param s the CC field of the Gamma record (see ENSDF manual).
     */
    public void getCC(String s){
        this.CC = s;
        setRecord();
    }
    /**
     * Sets the DCC field of the Gamma record (see ENSDF manual).
     * @param s the DCC field of the Gamma record (see ENSDF manual).
     */
    public void getDCC(String s){
        this.DCC = s;
        setRecord();
    }
    /**
     * Sets the TI field of the Gamma record (see ENSDF manual).
     * @param s the TI field of the Gamma record (see ENSDF manual).
     */
    public void getTI(String s){
        this.TI = s;
        setRecord();
    }
    /**
     * Sets the DTI field of the Gamma record (see ENSDF manual).
     * @param s the DTI field of the Gamma record (see ENSDF manual).
     */
    public void getDTI(String s){
        this.DTI = s;
        setRecord();
    }
    /**
     * Sets the C field of the Gamma record (see ENSDF manual).
     * @param s the C field of the Gamma record (see ENSDF manual).
     */
    public void setC(String s){
        this.C = s;
        setRecord();
    }
    /**
     * Sets the COIN field of the Gamma record (see ENSDF manual).
     * @param s the COIN field of the Gamma record (see ENSDF manual).
     */
    public void setCOIN(String s){
        this.COIN = s;
        setRecord();
    }
    /**
     * Sets the Q field of the Gamma record (see ENSDF manual).
     * @param s the Q field of the Gamma record (see ENSDF manual).
     */
    public void setQ(String s){
        this.Q = s;
        setRecord();
    }
    
    /**
     * Fills the 80 character array representing the Gamma record using the
     * data from the various fields.
     */
    private void setRecord(){
        int i;
        
        for(i=0; i<5; i++){
            this.record[i] = this.nucid.charAt(i);
        }
        this.record[5] = ' ';
        this.record[6] = ' ';
        this.record[7] = 'G';
        this.record[8] = ' ';
        
        super.fillRecord(this.E, 10, 19);
        super.fillRecord(this.DE, 20, 21);
        super.fillRecord(this.RI, 22, 29);
        super.fillRecord(this.DRI, 30, 31);
        super.fillRecord(this.M, 32, 41);
        super.fillRecord(this.MR, 42, 49);
        super.fillRecord(this.DMR, 50, 55);
        super.fillRecord(this.CC, 56, 62);
        super.fillRecord(this.DCC, 63, 64);
        super.fillRecord(this.TI, 65, 74);
        super.fillRecord(this.DTI, 75, 76);
        super.fillRecord(this.C, 77);
        super.fillRecord(this.COIN, 78);
        super.fillRecord(this.Q, 80);
    }
    
    /**
     * Returns the mass number for the nucid of this record.
     * @return the mass number for the nucid of this record.
     */
    public int getMassNumber(){
        int massNumber;
        String trimmedNucid = this.nucid.trim();
        String a = "";
        int i = 0;
        char c = trimmedNucid.charAt(i);
        while(c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9'){
            a += c;
            i += 1;
            c = trimmedNucid.charAt(i);
        }
        if(a.isEmpty()){
            massNumber = 0;
        }else{
            massNumber = Integer.parseInt(a);
        }
        return massNumber;
    }
    
    /** Returns the 80 character array <code>record</code> as a string object.
     *
     * @return the 80 character array  <code>record</code> as a string object.
     */
    @Override public String toString(){
        return new String(this.record);
    }
    
    /**
     * Returns <code>true</code> when all the properties of both Gamma
     * Records are the same.
     * @param other the ENSDFGammaRecord to compare with.
     * @return <code>true</code> when all the properties of both Gamma
     * Records are the same.
     */
    public boolean equals(ENSDFGammaRecord other){
        boolean result = true;
        result = result && nucid.equals(other.nucid);
        result = result && E.equals(other.E);
        result = result && DE.equals(other.DE);
        result = result && RI.equals(other.RI);
        result = result && DRI.equals(other.DRI);
        result = result && M.equals(other.M);
        result = result && MR.equals(other.MR);
        result = result && DMR.equals(other.DMR);
        result = result && CC.equals(other.CC);
        result = result && DCC.equals(other.DCC);
        result = result && TI.equals(other.TI);
        result = result && DTI.equals(other.DTI);
        result = result && C.equals(other.C);
        result = result && COIN.equals(other.COIN);
        result = result && Q.equals(other.Q);
        
        return result;
    }
    
    /**
     * Returns <code>true</code> when the following properties of both Gamma
     * Records are the same: NUCID, E, DE, RI, DRI, M, MR, Q.
     * @param other the ENSDFGammaRecord to compare with.
     * @return <code>true</code> when all the following properties of both Gamma
     * Records are the same: NUCID, E, DE, RI, DRI, M, MR, Q.
     */
    public boolean partialEquals(ENSDFGammaRecord other){
        boolean result = true;
        result = result && nucid.equals(other.nucid);
        result = result && E.equals(other.E);
        result = result && DE.equals(other.DE);
        result = result && RI.equals(other.RI);
        result = result && DRI.equals(other.DRI);
        result = result && M.equals(other.M);
        result = result && MR.equals(other.MR);
        result = result && Q.equals(other.Q);
        
        return result;
    }
}
