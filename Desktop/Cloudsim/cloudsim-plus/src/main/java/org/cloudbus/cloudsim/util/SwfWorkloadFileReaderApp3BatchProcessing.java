/**
 * 
 */
package org.cloudbus.cloudsim.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

import org.cloudbus.cloudsim.AutonomicLoadManagementStrategies.InitializationDC2;
import org.cloudbus.cloudsim.AutonomicLoadManagementStrategies.InitializationDC2BatchProcessing;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.EventInfo;

/**
 * @author Shyam Sundar V
 *
 */
public class SwfWorkloadFileReaderApp3BatchProcessing extends TraceReaderAbstract {
	   /**
     * Field index of job number.
     * Jub number values start from 1.
     */
    private static final int JOB_NUM_INDEX = 0;

    /**
     * Field index of submit time of a job (in seconds).
     */
    private static final int SUBMIT_TIME_INDEX = 1;

    /**
     * Field index of execution time of a job (in seconds).
     * The wall clock time the job was running (end time minus start time).
     */
    private static final int RUN_TIME_INDEX = 3;

    /**
     * Field index of number of processors needed for a job.
     * In most cases this is also the number of processors the job uses; if the job does not use all of them, we typically don't know about it.
     */
    private static final int NUM_PROC_INDEX = 4;

    /**
     * Field index of required number of processors.
     */
    private static final int REQ_NUM_PROC_INDEX = 7;

    /**
     * Field index of required running time.
     * This can be either runtime (measured in wallclock seconds), or average CPU time per processor (also in seconds)
     * -- the exact meaning is determined by a header comment.
     * If a log contains a request for total CPU time, it is divided by the number of requested processors.
     */
    private static final int REQ_RUN_TIME_INDEX = 8;

    /**
     * Field index of user who submitted the job.
     */
    private static final int USER_ID_INDEX = 11;

    /**
     * Field index of group of the user who submitted the job.
     */
    private static final int GROUP_ID_INDEX = 12;
    
    /**
     * Field index indicates if a job is Batch job or Interactive job.
     */
    private static final int BATCH_OR_INTERACTIVE = 15;

    /**
     * Max number of fields in the trace reader.
     */
    private static final int FIELD_COUNT = 18;

    /**
     * If the field index of the job number ({@link #JOB_NUM_INDEX}) is equals to this
     * constant, it means the number of the job doesn't have to be gotten from
     * the trace reader, but has to be generated by this workload generator class.
     */
    private static final int IRRELEVANT = -1;

    /**
     * @see #getMips()
     */
    private int mips;

    /**
     * List of Cloudlets created from the trace {@link #getInputStream()}.
     */
 //   private final List<Cloudlet> cloudletsDC2;

    /**
     * @see #setPredicate(Predicate)
     */
    private Predicate<Cloudlet> predicate;

    /**
     * Gets a {@link SwfWorkloadFileReader} instance from a workload file
     * inside the <b>application's resource directory</b>.
     * Use the available constructors if you want to load a file outside the resource directory.
     *
     * @param fileName the workload trace <b>relative file name</b> in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @param mips     the MIPS capacity of the PEs from the VM where each created Cloudlet is supposed to run.
     *                 Considering the workload reader provides the run time for each
     *                 application registered inside the reader, the MIPS value will be used
     *                 to compute the {@link Cloudlet#getLength() length of the Cloudlet (in MI)}
     *                 so that it's expected to execute, inside the VM with the given MIPS capacity,
     *                 for the same time as specified into the workload reader.
     * @throws IllegalArgumentException when the workload trace file name is null or empty; or the resource PE mips <= 0
     * @throws UncheckedIOException     when the file cannot be accessed (such as when it doesn't exist)
     */
    public static SwfWorkloadFileReaderApp3BatchProcessing getInstance(final String fileName, final int mips) {
        final InputStream reader = ResourceLoader.newInputStream(fileName, SwfWorkloadFileReaderDC2.class);
        return new SwfWorkloadFileReaderApp3BatchProcessing(fileName, reader, mips);
    }

    /**
     * Create a new SwfWorkloadFileReader object.
     *
     * @param filePath the workload trace file path in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @param mips     the MIPS capacity of the PEs from the VM where each created Cloudlet is supposed to run.
     *                 Considering the workload reader provides the run time for each
     *                 application registered inside the reader, the MIPS value will be used
     *                 to compute the {@link Cloudlet#getLength() length of the Cloudlet (in MI)}
     *                 so that it's expected to execute, inside the VM with the given MIPS capacity,
     *                 for the same time as specified into the workload reader.
     * @throws IllegalArgumentException when the workload trace file name is null or empty; or the resource PE mips <= 0
     * @throws FileNotFoundException    when the file is not found
     * @see #getInstance(String, int)
     */
    public SwfWorkloadFileReaderApp3BatchProcessing(final String filePath, final int mips) throws IOException {
        this(filePath, Files.newInputStream(Paths.get(filePath)), mips);
    }

    /**
     * Create a new SwfWorkloadFileReader object.
     *
     * @param filePath the workload trace file path in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @param reader   a {@link InputStreamReader} object to read the file
     * @param mips     the MIPS capacity of the PEs from the VM where each created Cloudlet is supposed to run.
     *                 Considering the workload reader provides the run time for each
     *                 application registered inside the reader, the MIPS value will be used
     *                 to compute the {@link Cloudlet#getLength() length of the Cloudlet (in MI)}
     *                 so that it's expected to execute, inside the VM with the given MIPS capacity,
     *                 for the same time as specified into the workload reader.
     * @throws IllegalArgumentException when the workload trace file name is null or empty; or the resource PE mips <= 0
     * @see #getInstance(String, int)
     */
    private SwfWorkloadFileReaderApp3BatchProcessing(final String filePath, final InputStream reader, final int mips) {
        super(filePath, reader);

        this.setMips(mips);
  //      this.CloudletQueueDC2 = new LinkedList<>();

        /*
        A default predicate which indicates that a Cloudlet will be
        created for any job read from the workload reader.
        That is, there isn't an actual condition to create a Cloudlet.
        */
        this.predicate = cloudlet -> true;
        
    }

    /**
     * Generates a list of jobs ({@link Cloudlet Cloudlets}) to be executed,
     * if it wasn't generated yet.
     *
     * @return a generated Cloudlet list
     */
    DatacenterBroker broker;
    CloudSim simulation;
    Queue<Cloudlet> CloudletQueueDC2;
    public Queue<Cloudlet> generateWorkload(DatacenterBroker broker,CloudSim simulation,Queue<Cloudlet> CloudletQueueDC2) {
    	this.CloudletQueueDC2=CloudletQueueDC2;
    	this.broker=broker;
    	this.simulation=simulation;
//        if (CloudletQueueDC2.isEmpty()) {
        	
            readFile(this::createCloudletFromTraceLine);
 //       }

        return CloudletQueueDC2;
    }

    
   
    /**
     * Defines a {@link Predicate} which indicates when a {@link Cloudlet}
     * must be created from a trace line read from the workload file.
     * If a Predicate is not set, a Cloudlet will be created for any line read.
     *
     * @param predicate the predicate to define when a Cloudlet must be created from a line read from the workload file
     * @return
     */
    public SwfWorkloadFileReaderApp3BatchProcessing setPredicate(final Predicate<Cloudlet> predicate) {
        this.predicate = predicate;
        return this;
    }
 
    
    /**
     * Extracts relevant information from a given array of fields, representing
     * a line from the trace reader, and creates a cloudlet using this
     * information.
     *
     * @param parsedLineArray an array containing the field values from a parsed trace line
     * @return true if the parsed line is valid and the Cloudlet was created, false otherwise
     */
    InitializationDC2BatchProcessing createVm=new InitializationDC2BatchProcessing();
    String ApplicationNumber="3";
    private boolean createCloudletFromTraceLine(final String[] parsedLineArray) {
    	//If all the fields couldn't be read, don't create the Cloudlet.
        if (parsedLineArray.length < FIELD_COUNT) {
            return false;
        }

        final int id = JOB_NUM_INDEX <= IRRELEVANT ? CloudletQueueDC2.size() + 1 : Integer.parseInt(parsedLineArray[JOB_NUM_INDEX].trim());

        /* according to the SWF manual, runtime of 0 is possible due
         to rounding down. E.g. runtime is 0.4 seconds -> runtime = 0*/
        final int runTime = Math.max(Integer.parseInt(parsedLineArray[RUN_TIME_INDEX].trim()), 1);

        /* if the required num of allocated processors field is ignored
        or zero, then use the actual field*/
        final int maxNumProc = Math.max(
                                    Integer.parseInt(parsedLineArray[REQ_NUM_PROC_INDEX].trim()),
                                    Integer.parseInt(parsedLineArray[this.NUM_PROC_INDEX].trim())
                               );
        final int numProc = Math.max(maxNumProc, 1);

        final long submitTime = Long.parseLong(parsedLineArray[this.SUBMIT_TIME_INDEX].trim());
        final int Batch=Integer.parseInt(parsedLineArray[BATCH_OR_INTERACTIVE].trim());
     
        if(Batch==2)
        {
        	if(submitTime<86400)//approximation for 1 day
        	{
        		long StartExecutionTime=86400;
        		Vm vm1=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm1);
        		Vm vm2=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,4);
        		broker.submitVm(vm2);
        		Vm vm3=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm3);
        		Vm vm4=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm4);
        		Vm vm5=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm5);
        		Cloudlet cloudlet1=createCloudletType1(runTime);
        		Cloudlet cloudlet2=createCloudletType2(runTime);
        		Cloudlet cloudlet3=createCloudletType3(runTime);
        		Cloudlet cloudlet4=createCloudletType4(runTime);
        		Cloudlet cloudlet5=createCloudletType3(runTime);
        		broker.bindCloudletToVm(cloudlet1, vm1);
        		broker.bindCloudletToVm(cloudlet2, vm2);
        		broker.bindCloudletToVm(cloudlet3, vm3);
        		broker.bindCloudletToVm(cloudlet4, vm4);
        		broker.bindCloudletToVm(cloudlet5, vm5);
        	}
        	
        	if(86400<submitTime && submitTime<172800)
        	{
        		long StartExecutionTime=172800;
        		Vm vm1=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm1);
        		Vm vm2=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,4);
        		broker.submitVm(vm2);
        		Vm vm3=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm3);
        		Vm vm4=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm4);
        		Vm vm5=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm5);
        		Cloudlet cloudlet1=createCloudletType1(runTime);
        		Cloudlet cloudlet2=createCloudletType2(runTime);
        		Cloudlet cloudlet3=createCloudletType3(runTime);
        		Cloudlet cloudlet4=createCloudletType4(runTime);
        		Cloudlet cloudlet5=createCloudletType3(runTime);
        		broker.bindCloudletToVm(cloudlet1, vm1);
        		broker.bindCloudletToVm(cloudlet2, vm2);
        		broker.bindCloudletToVm(cloudlet3, vm3);
        		broker.bindCloudletToVm(cloudlet4, vm4);
        		broker.bindCloudletToVm(cloudlet5, vm5);  
        		}
        	
        	if(172800<submitTime && submitTime<259200)
        	{
        		long StartExecutionTime=259200;
        		Vm vm1=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm1);
        		Vm vm2=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,4);
        		broker.submitVm(vm2);
        		Vm vm3=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm3);
        		Vm vm4=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,1);
        		broker.submitVm(vm4);
        		Vm vm5=createVm.createVmDC2(1,StartExecutionTime,ApplicationNumber,2);
        		broker.submitVm(vm5);
        		Cloudlet cloudlet1=createCloudletType1(runTime);
        		Cloudlet cloudlet2=createCloudletType2(runTime);
        		Cloudlet cloudlet3=createCloudletType3(runTime);
        		Cloudlet cloudlet4=createCloudletType4(runTime);
        		Cloudlet cloudlet5=createCloudletType3(runTime);
        		broker.bindCloudletToVm(cloudlet1, vm1);
        		broker.bindCloudletToVm(cloudlet2, vm2);
        		broker.bindCloudletToVm(cloudlet3, vm3);
        		broker.bindCloudletToVm(cloudlet4, vm4);
        		broker.bindCloudletToVm(cloudlet5, vm5); 
        		}
        }
        for(Cloudlet cloudlet : this.CloudletQueueDC2) {
        if(predicate.test(cloudlet)){
      //cloudletListDC2.add(cloudlet);[adds cloudlet 0 to the cloudletlist for every iteration and increases cludletlist count
            return true;
        }
        }
        return false;
    }
 
	  /**
	    * Creates a Cloudlet with the given information.
	    *
	    * @param id a Cloudlet ID
	    * @param broker of the data center 2
	    * @param OfflineTime of the cloudlet.offline time is the time cloudlet spends waiting 
	    * @return the created CloudletQueueDC2
	    * 
	    */
	List<Vm> FinishedvmListApplication3DC2 = new LinkedList<>();
	public Cloudlet createCloudletType1(int runTime) 
    {
        long fileSize = 300; 
        long outputSize = 300; 
        long mips=2500;
        int LetID = CloudletQueueDC2.size();
        int	pesNumber=1;
        long length=runTime;// (runTime * (mips*pesNumber));
    	Cloudlet cloudlet = new CloudletSimple(LetID, length, pesNumber)
    							.setFileSize(fileSize)
    							.setOutputSize(outputSize)
    							.setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
    							.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
    	CloudletQueueDC2.add(cloudlet); 
    	broker.submitCloudlet(cloudlet); 
    		
        cloudlet.addOnStartListener( info ->
        {
 		 	Vm VM=info.getCloudlet().getVm();
 		 	createVm.getvmQueueDC2().remove(VM);
 		 });
            
    	cloudlet.addOnFinishListener(info ->
    	{
    		Vm FinishedVm=cloudlet.getVm();
    		FinishedvmListApplication3DC2.add(FinishedVm);
    		cloudlet.setStatus(Cloudlet.Status.SUCCESS);
    		FinishedVm.getHost().destroyVm(FinishedVm);
    	});
    
    	return cloudlet;
    }
	
	public Cloudlet createCloudletType2(int runTime)
	{
        long fileSize = 300; 
        long outputSize = 300; 
        long mips=2500;
        int LetID = CloudletQueueDC2.size();       
    	int pesNumber=4;
    	long length= runTime;//(runTime * (mips*pesNumber));
    	Cloudlet cloudlet = new CloudletSimple(LetID, length, pesNumber)
    							.setFileSize(fileSize)
    							.setOutputSize(outputSize)
    							.setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
    							.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
	    CloudletQueueDC2.add(cloudlet); 
	    broker.submitCloudlet(cloudlet); 
	        
	    cloudlet.addOnStartListener( info ->
	    {
	    	Vm VM=info.getCloudlet().getVm();
 		 	createVm.getvmQueueDC2().remove(VM);
 		});
	
	    cloudlet.addOnFinishListener(info ->
	    {
	    	Vm FinishedVm=cloudlet.getVm();
			FinishedvmListApplication3DC2.add(FinishedVm);
			cloudlet.setStatus(Cloudlet.Status.SUCCESS);
			FinishedVm.getHost().destroyVm(FinishedVm);
	        });
	        
	    return cloudlet;
	}
	
	public Cloudlet	createCloudletType3(int runTime)
	{
        long fileSize = 300; 
        long outputSize = 300; 
        long mips=2500;
        int LetID = CloudletQueueDC2.size();	       
	    int pesNumber=2;
	    long length=runTime;// (runTime * (mips*pesNumber));
    	Cloudlet cloudlet = new CloudletSimple(LetID, length, pesNumber)
    							.setFileSize(fileSize)
    							.setOutputSize(outputSize)
    							.setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
    							.setUtilizationModelRam(new UtilizationModelDynamic(0.5)); 
    	CloudletQueueDC2.add(cloudlet); 
	    broker.submitCloudlet(cloudlet); 
	        
	   cloudlet.addOnStartListener( info ->
	   {
 			Vm VM=info.getCloudlet().getVm();
 			createVm.getvmQueueDC2().remove(VM);
 		});
	    
	   cloudlet.addOnFinishListener(info -> 
	   {
	       Vm FinishedVm=cloudlet.getVm();
	       FinishedvmListApplication3DC2.add(FinishedVm);
	       cloudlet.setStatus(Cloudlet.Status.SUCCESS);
	       FinishedVm.getHost().destroyVm(FinishedVm);
	   });
	
	   return cloudlet;
	}
    
	public Cloudlet createCloudletType4(int runTime)
	{
		long fileSize = 300; 
        long outputSize = 300; 
        long mips=2500;
        int LetID = CloudletQueueDC2.size();
        int	pesNumber=1;
        long length=runTime;// (runTime * (mips*pesNumber));
        
        Cloudlet cloudlet = new CloudletSimple(LetID, length, pesNumber)
    							.setFileSize(fileSize)
    							.setOutputSize(outputSize)
    							.setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
    							.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
        CloudletQueueDC2.add(cloudlet); 
        broker.submitCloudlet(cloudlet); 
    		
        cloudlet.addOnStartListener( info ->
        {
        	Vm VM=info.getCloudlet().getVm();
        	createVm.getvmQueueDC2().remove(VM);
        });
    	
        cloudlet.addOnFinishListener(info -> 
        {
    		Vm FinishedVm=cloudlet.getVm();
			FinishedvmListApplication3DC2.add(FinishedVm);
			cloudlet.setStatus(Cloudlet.Status.SUCCESS);
			FinishedVm.getHost().destroyVm(FinishedVm);
        });
    	
        return cloudlet;
	}
        
	public Cloudlet	createCloudletType5(int runTime) 
	{
		long fileSize = 300; 
		long outputSize = 300; 
		long mips=2500;
		int LetID = CloudletQueueDC2.size();	       
		int pesNumber=2;
		long length= runTime;//(runTime * (mips*pesNumber));
		
		Cloudlet cloudlet = new CloudletSimple(LetID, length, pesNumber)
	    		   				.setFileSize(fileSize)
	    		   				.setOutputSize(outputSize)
	    		   				.setUtilizationModelCpu(new UtilizationModelDynamic(0.1))
    							.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
		CloudletQueueDC2.add(cloudlet); 
		broker.submitCloudlet(cloudlet); 
        	
		cloudlet.addOnStartListener( info ->
		{
			Vm VM=info.getCloudlet().getVm();
			createVm.getvmQueueDC2().remove(VM);
		});
   	    
		cloudlet.addOnFinishListener(info ->
		{
   	       Vm FinishedVm=cloudlet.getVm();
   	       FinishedvmListApplication3DC2.add(FinishedVm);
   	       cloudlet.setStatus(Cloudlet.Status.SUCCESS);
   	       FinishedVm.getHost().destroyVm(FinishedVm);

		});
		
		return cloudlet;
    }

       
    /**
     * Gets the MIPS capacity of the PEs from the VM where each created Cloudlet is supposed to run.
     * Considering the workload reader provides the run time for each
     * application registered inside the reader, the MIPS value will be used
     * to compute the {@link Cloudlet#getLength() length of the Cloudlet (in MI)}
     * so that it's expected to execute, inside the VM with the given MIPS capacity,
     * for the same time as specified into the workload reader.
     */
    public int getMips() {
        return mips;
    }

    /**
     * Sets the MIPS capacity of the PEs from the VM where each created Cloudlet is supposed to run.
     * Considering the workload reader provides the run time for each
     * application registered inside the reader, the MIPS value will be used
     * to compute the {@link Cloudlet#getLength() length of the Cloudlet (in MI)}
     * so that it's expected to execute, inside the VM with the given MIPS capacity,
     * for the same time as specified into the workload reader.
     *
     * @param mips the MIPS value to set
     */
    public SwfWorkloadFileReaderApp3BatchProcessing setMips(final int mips) {
        if (mips <= 0) {
            throw new IllegalArgumentException("MIPS must be greater than 0.");
        }
        this.mips = mips;
        return this;
    }

}