package modules.vectorAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import common.VectorCalculation;
import common.parallelization.CallbackReceiver;
import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;

public class MinkowskiDistanceMatrixModule extends ModuleImpl {

	// Property keys
	private static final String PROPERTYKEY_INPUTFORMAT = "input format";
	public static final String PROPERTYKEY_DELIMITER_INPUT_REGEX = "input delimiter regex";

	// Define I/O IDs (must be unique for every input or output)
	private static final String ID_INPUT = "input";
	private static final String ID_OUTPUT = "output";

	private String inputFormat;
	private String inputdelimiter;

	public MinkowskiDistanceMatrixModule(CallbackReceiver callbackReceiver, Properties properties) throws Exception {

		// Call parent constructor
		super(callbackReceiver, properties);

		// Add module description
		this.setDescription("Calculates the minkowski distances for the specified Map of Sets.");

		// Add module category
		this.setCategory("Clustering");

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_DELIMITER_INPUT_REGEX,
				"Regular expression to use as segmentation delimiter for CSV input.");
		this.getPropertyDescriptions().put(PROPERTYKEY_INPUTFORMAT, "Format of input [json|csv].");

		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "Minkowski Distance Matrix");
		this.getPropertyDefaultValues().put(PROPERTYKEY_DELIMITER_INPUT_REGEX, "[\\,;]");
		this.getPropertyDefaultValues().put(PROPERTYKEY_INPUTFORMAT, "csv");

		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~). Every port
		 * can support a range of pipe types (currently byte or character
		 * pipes). Output ports can provide data to multiple pipe instances at
		 * once, input ports can in contrast only obtain data from one pipe
		 * instance.
		 */
		InputPort inputPort = new InputPort(ID_INPUT,
				"CSV or JSON formatted two-dimensional matrix (Map&lt;String,Set&lt;Double&gt;&gt;).", this);
		inputPort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(ID_OUTPUT,
				"JSON formatted distance matrix output (Map&lt;String,Map&lt;String,Double&gt;&gt;).", this);
		outputPort.addSupportedPipe(CharPipe.class);

		// Add I/O ports to instance (don't forget...)
		super.addInputPort(inputPort);
		super.addOutputPort(outputPort);

	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean process() throws Exception {

		// JSON parser
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Minkowski Distance matrix
		Map<String, Map<String, Double>> minkowskiDistanceMatrix = new HashMap<String, Map<String, Double>>();

		Map<String, List<Double>> aberrationValuesMap = null;
		
		if (this.inputFormat.equals("csv")){
			Scanner lineScanner = new Scanner(this.getInputPorts().get(ID_INPUT).getInputReader());
			lineScanner.useDelimiter("\\R+");
			
			aberrationValuesMap = new TreeMap<String, List<Double>>();
			
			// Read csv header line
			String line = lineScanner.next();
			String[] headerFields = line.split(this.inputdelimiter);
			
			for (int i=0; i<headerFields.length; i++){
				aberrationValuesMap.put(headerFields[i], new ArrayList<Double>());
			}
			
			while (lineScanner.hasNext()){
				line = lineScanner.next();
				
				String[] lineFields = line.split(this.inputdelimiter);
				String lineTag = lineFields[0];
				if (lineTag.isEmpty())
					continue;
				for (int i=1; i<lineFields.length; i++){
					if (lineFields[i].isEmpty())
						continue;
					aberrationValuesMap.get(lineTag).add(Double.parseDouble(lineFields[i]));
				}
				
			}
			
			lineScanner.close();
		} else {
			
		
		
		// Read input
		aberrationValuesMap = gson.fromJson(
				this.getInputPorts().get(ID_INPUT).getInputReader(), new HashMap<String, List<Double>>().getClass());
		
		}

		/*
		 * Iterate through map, removing the current item from it and comparing
		 * it to the remainder (to avoid comparing a pair twice [A-B and B-A] or
		 * an element to itself).
		 */
		Iterator<Entry<String, List<Double>>> types = aberrationValuesMap.entrySet().iterator();
		while (types.hasNext()) {

			// Remove entry
			Entry<String, List<Double>> entry = types.next();
			types.remove();

			// Create result map for current entry
			Map<String, Double> distanceMap = new HashMap<String, Double>();

			// Add result map to result matrix
			minkowskiDistanceMatrix.put(entry.getKey(), distanceMap);

			// Second level iteration to compare the current entry with the rest
			Iterator<Entry<String, List<Double>>> remainingTypes = aberrationValuesMap.entrySet().iterator();
			while (remainingTypes.hasNext()) {

				Entry<String, List<Double>> comparisonEntry = remainingTypes.next();

				// Calculate distance
				double distance = VectorCalculation.calculateMinkowskiDistance(entry.getValue(),
						comparisonEntry.getValue());

				// Store result
				distanceMap.put(comparisonEntry.getKey(), distance);

			}

		}

		// Prepare JSON output
		String jsonOutput = gson.toJson(minkowskiDistanceMatrix);

		// Write output
		this.getOutputPorts().get(ID_OUTPUT).outputToAllCharPipes(jsonOutput);

		// Close output port
		this.closeAllOutputs();

		return true;
	}

	@Override
	public void applyProperties() throws Exception {

		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();

		this.inputdelimiter = this.getProperties().getProperty(PROPERTYKEY_DELIMITER_INPUT_REGEX,
				this.getPropertyDefaultValues().get(PROPERTYKEY_DELIMITER_INPUT_REGEX));
		this.inputFormat = this.getProperties().getProperty(PROPERTYKEY_INPUTFORMAT,
				this.getPropertyDefaultValues().get(PROPERTYKEY_INPUTFORMAT));

		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}
