import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.file.DicomDictionary;
import gov.nih.mipav.model.file.FileDicomKey;
import gov.nih.mipav.model.file.FileDicomSQ;
import gov.nih.mipav.model.file.FileDicomTag;
import gov.nih.mipav.model.file.FileDicomTagInfo.VR;
import gov.nih.mipav.model.file.FileDicomTagTable;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.file.FileInfoDicom;
import gov.nih.mipav.model.file.FileUtility;
import gov.nih.mipav.model.file.FileWriteOptions;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.MipavUtil;

import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 * @author pandyan
 *
 */
public class PlugInAlgorithmNINDSInternalAnonymizationTool extends AlgorithmBase implements ActionListener, WindowListener {
	
	/** input directory path **/
	protected String inputDirectoryPath;
	
	/** output top level directory path **/
	protected String outputDirectoryPath;
	
	/** parent of patient list file path **/
	protected String parentPatientListFilePath;
	
	/** output text area **/
	protected JTextArea outputTextArea;
    
    /** boolean indicating if methods were successful **/
    protected boolean success = false;

    /** input image **/
    protected ModelImage inputImage;
    
    /** handle to FileIO **/
    protected FileIO fileIO;
    
    /** handle to FileInfoDicom **/
    protected FileInfoDicom fileInfoDicom;
    
    /** output File **/
    protected File outputFile;
    
    /** csv file **/
    protected File csvFile;
    
    /** output stream **/
    protected FileOutputStream outputStream;

	protected FileOutputStream outputStreamCSV;
    
    /** print stream **/
    protected PrintStream printStream;

	protected PrintStream printStreamCSV;
    
    /** outputText filename **/
    protected String outputTextFileName;
    
    /** message label **/
    protected JLabel errorMessageLabel;
    
    /** boolean if alg was canceled **/
    private boolean algCanceled = false;
            
    /** tag table **/
    protected FileDicomTagTable tagTable;
    
   /** newUID String **/
    protected String newUID;
    
    /** boolean if user hits ok or cancel */
    protected boolean pressedOK = false;
    
    /** calculated age if dob is to be supplied **/
    protected int calculatedAge = -1;
    
    /** age upper limit */
    private String AGE_UPPER_LIMIT = "90";
    
    /** is age >= AGE_UPPER_LIMIT */
    private boolean isMaxAge = false;
    
    /** hashmap of studyID and dobs **/
    protected HashMap<String, String> studyIdAndDOBHashMap = new HashMap<String, String>();
    
    /** hashmap of original and blinded patient id */
    private HashMap<String, String> patientListMap = new HashMap<String, String>();
    
    /** dates **/
    private Calendar dobCalendar, studyCalendar;
    
    /** todays date **/
    protected String todaysDateString;
    
    /** patientIDs that have been written out...unique based on patientID, studyID, and seriesNo**/
    protected Vector<String> donePatientIDs = new Vector<String>();
    
    /** csv file path **/
    protected String csvFilePath;
    
    /** Series folder name under process */
    private String seriesFolder = "";
    
    /** Study folder name under process */
    private String studyFolder = "";
    
    /** Patient folder name under process */
    private String patientFolder = "";
    
    /** Boolean indicating a new series folder */
    private boolean isNewSeries = false;
    
    /** Boolean indicating a new study folder */
    private boolean isNewStudy = false;
    
    /** Boolean indicating a new patient folder */
    private boolean isNewPatient = false;
    
    /** Patient id of previous patient folder */
    private String previousPatientId = "";
    
    /** STIR patient id */
    private String stirPatientId = "";
    
    /** File count in series, will be odd numbers 1,3,5,... */
    int fileCountInSeries = 1;
    
    /** Series count in study */
    int seriesCountInStudy = 2;
    
    /** Study count in patient */
    int studyCountInPatient = 1;
    
    /** Bogus SOPID */
    private String bogusSOPID = "";
    
    /** Media storage SOP UID */
    private String mediaStorageSOPUID = "";
    
    /** Bogus implementation ID */
    private String bogusImplementationID =  "1.2.840.34379.17";
    
    /** Fixed part of study/series instance UID */
    private String initialStudySeriesUID = "1.2.840.9999.9";
    
    /** Temporary fixed part of study/series instance UID, updated for each new patient */
    private String tempStudySeriesUID = "";
    
    /** Study instance UID */
    private String studyInstanceUID = "";
    
    /** Series instance UID */
	private String seriesInstanceUID = "";
	
	/** Boolean to skip patient for anonymization. Patients only in blinded csv file will be anonymized. */
	private boolean skipPatient = false;
	
	/** Total patients which is equal to total folders in input directory */
	private int totalPatients = 0;
    
	/** Total studies in each patient which is equal to total folders patient directory */
	private int totalStudies = 0;
	
	/** Total series in study which is equal to total folders in study directory */
	private int totalSeries = 0;
	
	/** Current patient, study and series in process */
	private int currentPatient, currentStudy, currentSeries = 0;
	
	/** Progress message string to indicate in process patient, study and series **/
    private String progressMsg;
		
    /** boolean indicating if csv file is new **/
    protected boolean newCSVFile;
    
    private static Vector<FileDicomKey> removeTagsVector;
    private static Vector<FileDicomKey> replaceTagsVector;
    private static Vector<FileDicomKey> encryptTagsVector;
    private static FileDicomKey key;
    
    protected static FileDicomKey patientIDKey;
    protected static FileDicomKey patientDOBKey;
    protected static FileDicomKey patientAgeKey;  
    protected static FileDicomKey studyDateKey;
    protected static FileDicomKey studyIDKey;
    protected static FileDicomKey seriesNoKey;
    protected static FileDicomKey sequenceNameKey;
    
    
    static {
    	removeTagsVector = new Vector<FileDicomKey>();
    	key = new FileDicomKey("0008,0021");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,0022");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,0023");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,0081");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,0092");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,0094");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0008,1155");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,0032");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,1000");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,1001");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,2180");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,21B0");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0010,4000");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0018,1030");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0032,1032");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0032,1033");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0032,1060");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0241");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0244");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0245");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0250");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0251");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0253");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0254");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,1001");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,0275");
    	removeTagsVector.add(key);
    	key = new FileDicomKey("0040,A730");
    	removeTagsVector.add(key);
    	
    	
    	
    	replaceTagsVector = new Vector<FileDicomKey>();
    	key = new FileDicomKey("0002,0003");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0002,0012");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0008,0012");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0008,0013");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0008,0014");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0008,0018");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0008,0020"); 
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0010,0010");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0010,0020");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0010,0030");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0010,1010");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0020,000D");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0020,000E");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0020,0052");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0020,0200");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0040,A124");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("0088,0140");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("3006,0024");
    	replaceTagsVector.add(key);
    	key = new FileDicomKey("3006,00C2");
    	replaceTagsVector.add(key);
    	
    	
    	encryptTagsVector = new Vector<FileDicomKey>();
    	key = new FileDicomKey("0008,0050");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,0080");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,0090");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1010");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1040");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1048");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1050");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1060");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1070");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0008,1090");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0018,1000");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0018,1016");
    	encryptTagsVector.add(key);
    	key = new FileDicomKey("0018,1018");
    	encryptTagsVector.add(key);
    	
    	
    	patientIDKey = new FileDicomKey("0010,0020");
    	patientDOBKey = new FileDicomKey("0010,0030");
    	patientAgeKey = new FileDicomKey("0010,1010");
    	studyDateKey = new FileDicomKey("0008,0020");
    	studyIDKey = new FileDicomKey("0020,0010");
    	seriesNoKey = new FileDicomKey("0020,0011");
    	sequenceNameKey = new FileDicomKey("0018,0024");

    }
    
    
	
	/**
	 * constructor
	 */
	public PlugInAlgorithmNINDSInternalAnonymizationTool(String inputDirectoryPath, String outputDirectoryPath, String parentPatientListFilePath, HashMap<String, String> patientListMap, JTextArea outputTextArea, JLabel errorMessageLabel, String csvFilePath, boolean newCSVFile) {
		this.inputDirectoryPath = inputDirectoryPath;
		this.outputDirectoryPath = outputDirectoryPath;
		this.parentPatientListFilePath = parentPatientListFilePath;
		this.patientListMap = patientListMap; 
		this.outputTextArea = outputTextArea;
		this.errorMessageLabel = errorMessageLabel;
		this.csvFilePath = csvFilePath;
		this.newCSVFile = newCSVFile;
		
		fileIO = new FileIO();
		fileIO.setQuiet(true);
		
		totalPatients = patientListMap.size();
		
		//remove last slash from input directory path if it has it
        if(String.valueOf(inputDirectoryPath.charAt(inputDirectoryPath.length() - 1)).equals(File.separator)) {
        	inputDirectoryPath = inputDirectoryPath.substring(0,inputDirectoryPath.length() - 1);
	    }
        //remove last slash from output directory  path if it has it
        if(String.valueOf(outputDirectoryPath.charAt(outputDirectoryPath.length() - 1)).equals(File.separator)) {
        	outputDirectoryPath = outputDirectoryPath.substring(0,outputDirectoryPath.length() - 1);
	    }
        
	}
	
	

	/**
	 * run algorithm
	 */
	public void runAlgorithm() {
		long begTime = System.currentTimeMillis();

		try {
			outputTextFileName = "output_" + System.currentTimeMillis() + ".txt";
			outputFile = new File(parentPatientListFilePath + File.separator + outputTextFileName);
        	outputStream = new FileOutputStream(outputFile);
        	printStream = new PrintStream(outputStream);
        	csvFile = new File(csvFilePath);
        	outputStreamCSV = new FileOutputStream(csvFile,true);
        	printStreamCSV = new PrintStream(outputStreamCSV);
        	if(newCSVFile) {
        		printStreamCSV.println("patientID,patientsAge,studyDate,studyID,seriesNo,todaysDate,sequenceName,blindedPatientID");
        	}
        	Calendar t = Calendar.getInstance();
    		SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
    		todaysDateString = sdf.format(t.getTime());
    		String tdmmString = todaysDateString.substring(0, 2);
			String tdddString = todaysDateString.substring(2,4);
			String tdyyyyString = todaysDateString.substring(4,todaysDateString.length());
			if(tdmmString.startsWith("0")) {
				tdmmString = tdmmString.substring(1,2);
			}
			if(tdddString.startsWith("0")) {
				tdddString = tdddString.substring(1,2);
			}
			todaysDateString = tdmmString + "/" + tdddString + "/" + tdyyyyString;
    	}catch(Exception e) {
        	System.out.println(e.getMessage());
        }
		
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        outputTextArea.append(dateFormat.format(date) + "\n\n");
        printStream.println(dateFormat.format(date));
        printStream.println();
        
        outputTextArea.append("** Beginning NINDS Anonymization **\n\n");
        printStream.println("** Beginning NINDS Anonymization **");
        printStream.println();
        
        // first create a File object based upon the input path
        File inputDirectoryRoot = new File(inputDirectoryPath);

        success = parse(inputDirectoryRoot);

        if (success == false) {
        	outputTextArea.append("! Algorithm Canceled \n");
        	errorMessageLabel.setText("! Algorithm Canceled");
        	printStream.println("! Algorithm Canceled");
        	finalize();
            setCompleted(true);
        }

        outputTextArea.append("** Ending NINDS Anonymization **\n\n");
        printStream.println("** Ending NINDS Anonymization **");
        printStream.println();



        long endTime = System.currentTimeMillis();
        long diffTime = endTime - begTime;
        float seconds = ((float) diffTime) / 1000;

       outputTextArea.append("Algorithm took " + seconds + " seconds \n");
       printStream.println("Algorithm took " + seconds + " seconds");
       
       finalize();
       setCompleted(true);
	}
	
	
	
	/**
	 * parses the input directory and anonymizes image files
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws OutOfMemoryError
	 */
	 public boolean parse(File file) {

	        File[] children = file.listFiles();
	        
	        for (int i = 0; i < children.length; i++) {

                if (algCanceled) {
                    return false;
                }

                if (children[i].isDirectory()) {
                	//create this directory in output directory if its not there yet
                    File outputDataFolder = new File(children[i].getAbsolutePath().replace(inputDirectoryPath, outputDirectoryPath));
                    if(!outputDataFolder.exists() && !skipPatient) {
                    	outputDataFolder.mkdir();
                    }
                                                           
                    parse(children[i]);
            		 
                              	
                } else if (!children[i].isDirectory()) {
                		try {
	                		if((FileUtility.isDicom(children[i].getName(), children[i].getParent() + File.separator, true) == FileUtility.DICOM) || (FileUtility.isDicom_ver2(children[i].getName(), children[i].getParent() + File.separator, true) == FileUtility.DICOM)) {
	                			String currentSeriesFolder = children[i].getParent().trim();
	                			String currentStudyFolder = children[i].getParentFile().getParent().trim();
	                			String currentPatientFolder = children[i].getParentFile().getParentFile().getParent().trim();
	                				                			
	                			// New patient
	                			if (!(currentPatientFolder.equalsIgnoreCase(patientFolder))) {
	                				isNewPatient = true;
	                				isNewStudy = true;
	                				isNewSeries = true;
	                				seriesFolder = currentSeriesFolder;
	                				studyFolder = currentStudyFolder;
	                				patientFolder = currentPatientFolder;
	                				totalStudies = children[i].getParentFile().getParentFile().getParentFile().listFiles().length;
	                				totalSeries = children[i].getParentFile().getParentFile().listFiles().length;
	                				currentStudy = 1;
	                				currentSeries = 1;
	                			} else if (currentPatientFolder.equalsIgnoreCase(patientFolder) && (!(currentStudyFolder.equalsIgnoreCase(studyFolder)))) {
	                				if (skipPatient == true) {
	                					break;
	                				}
	                				isNewPatient = false;
	                				isNewStudy = true;
	                				isNewSeries = true;
	                				seriesFolder = currentSeriesFolder;
	                				studyFolder = currentStudyFolder;
	                				totalSeries = children[i].getParentFile().getParentFile().listFiles().length;
	                				currentStudy += 1;
	                				currentSeries = 1;
	                				if (skipPatient == false) {
	                					progressMsg = "Processing Patient " + currentPatient + "/" + totalPatients + "  Study " + currentStudy + "/" + totalStudies + "  Series " + currentSeries + "/" + totalSeries;
		                				fireProgressStateChanged(0, "Image De-identification Progress", progressMsg);
	                				}
	                			} else if (currentPatientFolder.equalsIgnoreCase(patientFolder) && currentStudyFolder.equalsIgnoreCase(studyFolder) && (!(currentSeriesFolder.equalsIgnoreCase(seriesFolder)))) {
	                				if (skipPatient == true) {
	                					break;
	                				}
	                				isNewPatient = false;
	                				isNewStudy = false;
	                				isNewSeries = true;
	                				seriesFolder = currentSeriesFolder;
	                				currentSeries += 1;
	                				if (skipPatient == false) {
	                					progressMsg = "Processing Patient " + currentPatient + "/" + totalPatients + "  Study " + currentStudy + "/" + totalStudies + "  Series " + currentSeries + "/" + totalSeries;
		                				fireProgressStateChanged(0, "Image De-identification Progress", progressMsg);
	                				}
	                			} else {
	                				isNewPatient = false;
	                				isNewStudy = false;
	                				isNewSeries = false;
	                				if (skipPatient == false) {
	                					int progressValue = (i * 100)/children.length;
	                					fireProgressStateChanged(progressValue, "Image De-identification Progress", progressMsg);
	                				}
	                			}
	                			
	                			success = anonymizeDICOM(children[i]);
	                	        if (success == false) {
	                	        	if (skipPatient == true) {
	                	        		break;
	                	        	}
	                				outputTextArea.append("WARNING: Error in anonymizing. Skipping file " + children[i].getName() + " \n\n");
	                				printStream.println("WARNING: Error in anonymizing. Skipping file " + children[i].getName());
	                				printStream.println();
	                	            continue;
	                	        }
	                			
	                		}
                		}catch(IOException e) {
                			outputTextArea.append("WARNING: IO Error in determing if file is DICOM. Skipping file " + children[i].getName() + " \n\n");
                			printStream.println("WARNING: IO Error in determing if file is DICOM. Skipping file " + children[i].getName());
            				printStream.println();
            	            continue;
                		}
                	
                }
                
	        }
	        
	        return true;
	 }
	
	
	
	
	
	
	/**
	 * anonymize dicom images
	 * @return
	 */
	public boolean anonymizeDICOM(File file) {
		//read in image
		String absPath = file.getAbsolutePath();
		try {
			inputImage = fileIO.readImage(absPath);
			fileInfoDicom = (FileInfoDicom)inputImage.getFileInfo(0);
		} catch (Exception e) {
			outputTextArea.append("ERROR: Unexpected IO error occurred. Cannot read image " + absPath + ".\n");
			printStream.println("ERROR: Unexpected IO error occured. Cannot read image " + absPath);
			return false;
		}
		
		success = anonymizeDICOMTags(file);
		if (success == false) {
			fileInfoDicom.finalize();
			inputImage.disposeLocal();
	        inputImage = null;
            return false;
        }
		
		//save anonymized image
		String outputDir = file.getParent().replace(inputDirectoryPath, outputDirectoryPath);
		
		
		File outputFile = new File(outputDir);
		File parentFile = outputFile.getParentFile();
		File grandParentFile = parentFile.getParentFile();
		
		//rename patient folder with the anonymized patient id
		if(!(grandParentFile.getName().equals(newUID))) {
			String newName = grandParentFile.getParent() + File.separator + newUID;
			File dest = new File(newName);
			boolean succ = grandParentFile.renameTo(dest);
			//if succ is false, then a folder already exists...so delete this dir
			if(succ == false) {

				delete(grandParentFile);
			}
			outputDir = newName + File.separator + parentFile.getName() + File.separator + outputFile.getName(); 
		}
						
		FileWriteOptions opts = new FileWriteOptions(true);
        opts.setFileType(FileUtility.DICOM);
        opts.setFileDirectory(outputDir + File.separator);
        opts.setFileName(mediaStorageSOPUID + ".dcm");
        opts.setRecalculateInstanceNumber(false);
        opts.doStamp(false); //NINDS would prefer to not place references to NIH
        
        outputTextArea.append("Saving " + mediaStorageSOPUID + ".dcm" + " to " + outputDir + " \n\n");
        printStream.println("Saving " + mediaStorageSOPUID + ".dcm" + " to " + outputDir);
        printStream.println();
        
        //NINDS would prefer to remove all existing references to NIH
        if (inputImage.isDicomImage()) {
            FileInfoDicom fileInfo = (FileInfoDicom) inputImage.getFileInfo(0);
            fileInfo.removeStampSecondaryCapture();
        }
        
        fileIO.setDisplayRangeOfSlicesDialog(false);
        fileIO.writeImage(inputImage, opts);
        

        inputImage.disposeLocal();
        inputImage = null;
        fileInfoDicom = null;
        
		return true;
	}
	
	
	/**
	 * anonymize dicom images
	 * this method does the actual anonymizing part
	 * @return
	 */
    public boolean anonymizeDICOMTags(File file) {
    	tagTable = fileInfoDicom.getTagTable();
    	String studyID = ((String)tagTable.getValue(studyIDKey)).trim();
    	String seriesNo = ((String)tagTable.getValue(seriesNoKey)).trim();
    	String sequenceName = "";
    	if(tagTable.getValue(sequenceNameKey) != null) {
    		sequenceName = ((String)tagTable.getValue(sequenceNameKey)).trim();
    	}
    	    	
    	boolean validDOB = true;
    	String studyDate = "";
    	int size;
    	
    	String patientID = "";
    	String dob = "";
    	    	
    	if(tagTable.containsTag(patientIDKey)) {
    		patientID = ((String)tagTable.getValue(patientIDKey)).trim();
    		
    	}
    	
    	//Patient ID tag is replaced by STIR ID. 
    	
    	if (patientListMap.containsKey(patientID)) {
    		skipPatient = false;
    		
    		if (!patientID.equals(previousPatientId)) {
    			boolean isIdUnique = false;
        		while (!isIdUnique) {
        			isIdUnique = generateStirId(patientID);
        		}
        		if (isIdUnique) {
        			newUID = stirPatientId;
        		} else {
        			File skippedPatientFolder = new File(file.getParentFile().getParentFile().getParentFile().getAbsolutePath().replace(inputDirectoryPath, outputDirectoryPath));
        			delete(skippedPatientFolder);
        			setAlgCanceled(true);
        			return false;
        		}
        		
    			previousPatientId = patientID;
    			currentPatient += 1;
    			progressMsg = "Processing Patient " + currentPatient + "/" + totalPatients + "  Study " + currentStudy + "/" + totalStudies + "  Series " + currentSeries + "/" + totalSeries;
				fireProgressStateChanged(0, "Image De-identification Progress", progressMsg);
    		}
    		outputTextArea.append("Anonymizing " + inputImage.getImageFileName() + " from " + inputImage.getImageDirectory() + " \n");
    		printStream.println("Anonymizing " + inputImage.getImageFileName() + " from " + inputImage.getImageDirectory());
    	
		} else {
			skipPatient = true;
			File skippedPatientFolder = new File(file.getParentFile().getParentFile().getParentFile().getAbsolutePath().replace(inputDirectoryPath, outputDirectoryPath));
			delete(skippedPatientFolder);
			return false;
		}
				
		studyDate = ((String)tagTable.getValue(studyDateKey)).trim();
		if(studyDate.contains("/")) {
			studyDate = studyDate.replaceAll("\\/", "");
		}
		if(studyDate.length() != 8) {
			outputTextArea.append("WARNING: Study Date tag (0008,0020) value is not a valid entry. \n");
			printStream.println("WARNING: Study Date tag (0008,0020) value is not a valid entry.");
			return false;
		}
		String sdmmString = studyDate.substring(0, 2);
		String sdddString = studyDate.substring(2, 4);
		String sdyyyyString = studyDate.substring(4, 8);
		int sdmm = Integer.valueOf(sdmmString);
		int sddd = Integer.valueOf(sdddString);
		int sdyyyy = Integer.valueOf(sdyyyyString);
		
		if(tagTable.containsTag(patientDOBKey)) {
			dob = ((String)tagTable.getValue(patientDOBKey)).trim();
			if(dob.contains("/")) {
				dob = dob.replaceAll("\\/", "");
			}
		}
		//dob length is 8  MMYYDDDD
		//dob might be written in "years"...but it also might have its length as 8...so test for that
		if(dob.equals("")) {
			validDOB = false;
		}else {
			if(dob.length() == 8) {
				try{
					@SuppressWarnings("unused")
					int dobInt = Integer.valueOf(dob);
					validDOB = true;
				}catch(NumberFormatException e) {
					validDOB = false;
				}
			}else {
				validDOB = false;
			}
		}
		
		//New patient is taken from blinded patient id map
		
		/*if (blindedPatientIdMap.containsKey(patientID)) {
			newUID = (String)blindedPatientIdMap.get(patientID);
		} else {
			outputTextArea.append("ERROR: Algorithm Canceled \n");
        	errorMessageLabel.setText("ERROR: The original patient id in blinding csv file does not match the patient id in image!");
        	printStream.println("ERROR: Algorithm Canceled");
        	
        	fatalError = true;
            return false;
		}*/
		
		//if dob is there and in right format , create newUID using this field....otherwise if dob is not there
		if(validDOB) {
			String mmString = dob.substring(0, 2);
			String ddString = dob.substring(2,4);
			String yyyyString = dob.substring(4,dob.length());
			if(mmString.startsWith("0")) {
				mmString = mmString.substring(1,2);
			}
			if(ddString.startsWith("0")) {
				ddString = ddString.substring(1,2);
			}
			int mm = Integer.valueOf(mmString);
			int dd = Integer.valueOf(ddString);
			int yyyy = Integer.valueOf(yyyyString);
			dobCalendar = Calendar.getInstance();
			dobCalendar.set(yyyy, mm, dd);
			studyCalendar = Calendar.getInstance();
			studyCalendar.set(sdyyyy, sdmm, sddd);
			calculatedAge = calculateAge(dobCalendar,studyCalendar);
			
		}

    	//one will get replaced with current date...so get current date
    	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        String currentDate = dateFormat.format(date);

        //one tag will get replaced with current time...so get that
        DateFormat timeFormat = new SimpleDateFormat("HHmmss");
        date = new Date();
        String currentTime = timeFormat.format(date);
        
        if (isNewPatient) {
        	studyCountInPatient = 1;
        	seriesCountInStudy = studyCountInPatient + 1;
        	tempStudySeriesUID = initialStudySeriesUID + "." + System.currentTimeMillis() + ".";
        } else if ((!isNewPatient) && isNewStudy) {
        	studyCountInPatient += 1;
        	seriesCountInStudy = studyCountInPatient + 1;
        } else if ((!isNewPatient) && (!isNewStudy) && isNewSeries) {
        	seriesCountInStudy += 1;
        }
        studyInstanceUID = tempStudySeriesUID + Integer.toString(studyCountInPatient);
    	seriesInstanceUID = tempStudySeriesUID + Integer.toString(seriesCountInStudy);
    	
        // generate media storage sop uid which is eventually the file name of the image. 
        
        if (isNewSeries) {
        	fileCountInSeries = 1;
        	bogusSOPID = bogusImplementationID + "." + System.currentTimeMillis() + ".";
        	mediaStorageSOPUID = (bogusSOPID + Integer.toString(fileCountInSeries)).trim();
        	fileCountInSeries += 2;
        } else {
        	mediaStorageSOPUID = (bogusSOPID + Integer.toString(fileCountInSeries)).trim();
        	fileCountInSeries += 2;
        }
                     
        //MD5
        MessageDigest digest = null;
        try {
        	digest = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException e) {
        	outputTextArea.append("WARNING: Error in MD5 hash algorithm.\n");
        	printStream.println("WARNING: Error in MD5 hash algorithm.");
        	e.printStackTrace();
        	return false;
        }

        //patient's birthdate (0010,0030) will be replaced with patients age...so get patients age
        String patientsAge = "";
        int age = 0;
        if(tagTable.containsTag(patientAgeKey)) {
        	if(calculatedAge != -1) {
        		patientsAge = String.valueOf(calculatedAge);
        	} else {
        		patientsAge = ((String)tagTable.getValue(patientAgeKey)).trim();
        		if (patientsAge.startsWith("0")) {
        			patientsAge = patientsAge.substring(0, 3).trim();
        			try {
            			age = Integer.parseInt(patientsAge);
            		} catch (NumberFormatException nfe_withzero) {
            			patientsAge = patientsAge.substring(0, 2).trim();
            			try {
            				age = Integer.parseInt(patientsAge);
            			} catch (NumberFormatException nfe) {
            				    outputTextArea.append("WARNING: Unable to read patients age from tag.\n");
            					printStream.println("WARNING: Unable to read patients age from tag.");
            					nfe.printStackTrace();
            					return false;
            			}
            			
            		}
        		} else {
        			patientsAge = patientsAge.substring(0, 2);
        			try {
            			age = Integer.parseInt(patientsAge);
            		} catch (NumberFormatException nfe_withoutzero) {
            			patientsAge = patientsAge.substring(0, 1);
            			try {
            				age = Integer.parseInt(patientsAge);
            			} catch (NumberFormatException nfe) {
            				outputTextArea.append("WARNING: Unable to read patients age from tag.");
            				printStream.println("WARNING: Unable to read patients age from tag.");
            				nfe.printStackTrace();
            				return false;
            				
            			}
            		}
        			
        		}
        		     		        		
        		if (age >= 90) {
        			patientsAge = AGE_UPPER_LIMIT;
        			isMaxAge = true;
        		} else {
        			isMaxAge = false;
        		}
        		
        	}
        }
        
        //Study Instance UID (0020,000D) and Series Instance UID (0002,000E) need MIPAV version and time in milliseconds
        String mipavVersion = MipavUtil.getVersion();
        mipavVersion = mipavVersion.replaceAll("\\.", "");
           	    	    		
    	//write out csvFile
    	String csvCheck = patientID + studyID + seriesNo;
    	if(!donePatientIDs.contains(csvCheck)) {
    		//remove beginning zeros for study date and todays date when writing to csv
        	if(sdmmString.startsWith("0")) {
    			sdmmString = sdmmString.substring(1,2);
    		}
    		if(sdddString.startsWith("0")) {
    			sdddString = sdddString.substring(1,2);
    		}
    		studyDate = sdmmString + "/" + sdddString + "/" + sdyyyyString;
    		
    		//write out to csv
    		printStreamCSV.println(patientID + "," + patientsAge + "," + studyDate + "," + studyID + "," + seriesNo + "," + todaysDateString + "," + sequenceName + "," + newUID);
    		donePatientIDs.add(csvCheck);
    	}

    	//replace specific defined tags
    	size = replaceTagsVector.size();
    	String s;
    	for (int i = 0; i < size; i++) {
        	FileDicomKey key = replaceTagsVector.get(i);
        	if(tagTable.containsTag(key)) {
        		String keyString = key.getKey();
        		if(keyString.equals("0002,0003")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, mediaStorageSOPUID);
        		}else if(keyString.equals("0002,0012")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, bogusImplementationID);
        		}else if(keyString.equals("0008,0012")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, currentDate);
        		}else if(keyString.equals("0008,0013")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, currentTime);
        			
        		}else if(keyString.equals("0008,0014")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, bogusImplementationID);
        		}else if(keyString.equals("0008,0018")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, mediaStorageSOPUID);
        		}else if(keyString.equals("0008,0020")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			String k = s.substring(0, s.lastIndexOf("/")+1) + "1900";
            		tagTable.setValue(key, k);
        		}else if(keyString.equals("0010,0010")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, newUID);
        		}else if(keyString.equals("0010,0020")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, newUID);
        		}else if(keyString.equals("0010,0030")) {
        			//dont need to do anything id dob was entered as age
        			if(validDOB || (!validDOB && dob.equals(""))) {
	        			s = (String)tagTable.getValue(key);
	        			if(s.trim().equals("")) {
	        				s = "EMPTY VALUE";
	        			}
	        			tagTable.setValue(key, patientsAge);
        			}
        		} else if (keyString.equals("0010,1010") && (isMaxAge)) {
        			s = (String)tagTable.getValue(key);
        			if (s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, patientsAge + " Years");
        			
        		} else if(keyString.equals("0020,000D")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, studyInstanceUID);
        		} else if(keyString.equals("0020,000E")) {
        			s = (String)tagTable.getValue(key);
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			tagTable.setValue(key, seriesInstanceUID);
        		}else if(keyString.equals("0020,0052")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        			
        		}else if(keyString.equals("0020,0200")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        			
        		}else if(keyString.equals("0040,A124")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        			
        		}else if(keyString.equals("0088,0140")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        		}else if(keyString.equals("3006,0024")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        		}else if(keyString.equals("3006,00C2")) {
        			s = ((String)tagTable.getValue(key)).trim();
        			if(s.trim().equals("")) {
        				s = "EMPTY VALUE";
        			}
        			int x = s.lastIndexOf(".");
        			String n = "";
            		if(x != -1) {
            			String k = s.substring(0, x);
            			n = k + ".99999";
            		}else {
            			//need to figure out what to do here
            			//for now...just seet it to 99999
            			n = "99999";
            		}
            		tagTable.setValue(key, n);
        		}
        	}
    	}
    	
    	
    	
    	
    	//encrypt specific defined tags
    	size = encryptTagsVector.size();
    	for (int i = 0; i < size; i++) {
        	FileDicomKey key = encryptTagsVector.get(i);
        	
	    	if((tagTable.containsTag(key)) && (!(((String)tagTable.getValue(key)).trim()).equals(""))) {
	    		s = ((String)tagTable.getValue(key)).trim();
	    		digest.update(s.getBytes());
	    		byte[] encBytes = digest.digest();
	    		StringBuffer hexString = new StringBuffer();
	    		for(int k=0;k<encBytes.length;k++) {
	    			hexString.append(Integer.toHexString(0xFF & encBytes[k]));
	    		}
	    		tagTable.setValue(key.getKey(), hexString);
	    	} 
    	
    	}
    	
    	

    	//remove specific defined tags
    	size = removeTagsVector.size();
        for (int i = 0; i < size; i++) {
        	FileDicomKey key = removeTagsVector.get(i);
        	if(tagTable.containsTag(key)) {
        		tagTable.removeTag(key);
        	}
        }
        

        //remove private tags
    	Hashtable<FileDicomKey, FileDicomTag> fullTagsList = tagTable.getTagList();
    	Enumeration<FileDicomKey> e = fullTagsList.keys();
    	while (e.hasMoreElements()) {
            FileDicomKey tagKey = e.nextElement();
            String tag = tagKey.getKey();
    		String k = tag.substring(0, 4);
    		try {
    			int n = new Integer(k).intValue();
    			if(n%2 != 0) {
    				tagTable.removeTag(tag);
    			}
    		}catch(NumberFormatException nfe) {
    			//do nothing
    		} 
    	}
    	
    	
    	
    	//remove sequence tags
    	Enumeration<FileDicomKey> e2 = fullTagsList.keys();
    	while(e2.hasMoreElements()) {
    		FileDicomKey tagKey = e2.nextElement();
    		String tag = tagKey.getKey();
    		VR type = DicomDictionary.getType(tagKey);
    		if (type == null) {
                type = VR.UN;
    		}
    		if (type.equals(VR.SQ)) {
    			tagTable.removeTag(tag);
    		}
    		if(type.equals(VR.UN)) {
				if(tagTable.getValue(tagKey) instanceof FileDicomSQ) {
					tagTable.removeTag(tag);	
				}

    		}
    	}
    	
    	
    	
    	return true;
    	
    }
	
	
	private int calculateAge(Calendar dob, Calendar studyDate) {
		int age = 0;
		
		age = studyDate.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
		if (studyDate.get(Calendar.DAY_OF_YEAR) <= dob.get(Calendar.DAY_OF_YEAR)) {
			age--;
		}
		
		// Age need to have an upper limit placed on it. Anyone with a calculated 
		// age of >= 90 should have age (reported in DOB tag) as 90. 
		
		if (age >= 90) {
			isMaxAge = true;
			return 90;
		} else {
			isMaxAge = false;
			return age;
		}
	}
	
	/**
	 * Generate randomized STIR ID. 
	 * @return
	 */
	private boolean generateStirId(String interimId) {
		
		int id,month; 
		String [] mmddyyyy = todaysDateString.split("/");
		Random randomGenerator = new Random();
		int randomInt;
		
		try {
			month = Integer.valueOf(mmddyyyy[0]);
		} catch (NumberFormatException nfe) {
			return false;
		}
		
		try {
			id = Integer.valueOf(interimId);
		} catch (NumberFormatException nfe) {
			return false;
		}
		
		randomInt = randomGenerator.nextInt(101);
		
		stirPatientId = (month * id) + mmddyyyy[1] + randomInt + mmddyyyy[2].substring(2, mmddyyyy[2].length());
		boolean success = checkForUniqueId(stirPatientId);
		if (!success) {
			return false;
		}
		return true;
	}
	
	/**
	 * Checks the generated stir id for uniqueness. 
	 * @return
	 */
	private boolean checkForUniqueId(String id) {
		
		String idDbFileFolder = System.getProperty("user.home");
		//String idDbFileFolder = "C:" + File.separator + "StirDBTemp";
		String idDbFileName = idDbFileFolder + File.separator + "StirIdDb.txt";	
		HashMap<String, String> existingIds = new HashMap<String, String>();
		try {
			File idDbFile = new File(idDbFileName);
			
			// Create file if it does not exist
			boolean success = idDbFile.createNewFile();
			
			if (success) { 
				// File did not exist and was created. 
				try {
			        BufferedWriter out = new BufferedWriter(new FileWriter(idDbFileName));
			        out.write(id + System.getProperty("line.separator"));
			        out.close();
			    } catch (IOException e2) {
			    	System.out.println("e2 exception occurred.");
			    }
			} else {
				BufferedReader reader = new BufferedReader(new FileReader(idDbFileName));
				String line = "";
				try {
	        		while((line = reader.readLine()) != null) {
	            		existingIds.put(line.trim(), "");
	            	}
	        	} catch (IOException e4) {
	        		System.out.println("e4 exception occurred.");
	        	}
	        	if (existingIds.containsKey(id)){
	        		return false;
	        	} else {
	        		try {
				        BufferedWriter out = new BufferedWriter(new FileWriter(idDbFileName, true));
				        out.write(id + System.getProperty("line.separator"));
				        out.close();
				    } catch (IOException e3) {
				    	System.out.println("e3 exception occurred.");
				    }
	        	}
			}
		} catch (IOException e1) {
			System.out.println("e1 exception occurred.");
		}
		
		return true;
		
		
	}
	
	
	/**
	 * Deletes files and folders including sub folders. 
	 * @return
	 */
	public boolean delete(File file) {
    	if(file.isDirectory()) {
    		File[] children = file.listFiles();
    		for(int i=0;i<children.length;i++) {
    			if(children[i].isDirectory()) {
    				boolean success = delete(children[i]);
    				if(!success) {
    					return false;
    				}
    			}else {
    				return false;
    			}

    		}
    	}else {
    		return false;
    	}
    	
    	return file.delete();
    }


	/**
	 * get output text filename
	 * @return
	 */
	public String getOutputTextFileName() {
		return outputTextFileName;
	}


	/**
	 * get output directory path
	 * @return
	 */
	public String getInputDirectoryPath() {
		return inputDirectoryPath;
	}


	/**
	 * set alg canceled
	 * @param algCanceled
	 */
	public void setAlgCanceled(boolean algCanceled) {
		this.algCanceled = algCanceled;
	}

}
