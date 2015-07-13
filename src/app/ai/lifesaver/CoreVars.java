package app.ai.lifesaver;

public class CoreVars {

	 //training and monitoring variables
     static int queueSize = 1000; //how many magnitudes are required for training and monitoring
     static int alphaCount = queueSize; //how many alphas we should simulate
     static double subsize = 2/3.0; //percent of queue size data is subsampled for computing alphas
     static double dev, key; //standard derivation and mean of training data
     boolean magFLag; //true when eyes movement deviates from baseline training data
    
}
