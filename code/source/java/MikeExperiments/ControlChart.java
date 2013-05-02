// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import MikeExperiments.uq.FeaturizeDataHMM;

import web.engr.oregonstate.edu.zheng.gef.def.Array;
import web.engr.oregonstate.edu.zheng.gef.def.Table;
import web.engr.oregonstate.edu.zheng.gef.def.TableColumn;
import web.engr.oregonstate.edu.zheng.gef.def.TaskDef;
import web.engr.oregonstate.edu.zheng.gef.def.Var;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder.VerificationType;
import web.engr.oregonstate.edu.zheng.gef.task.freeliving.Formula;
import web.engr.oregonstate.edu.zheng.gef.utils.RUtils;
import web.engr.oregonstate.edu.zheng.obesity.commons.utils.DataSet;


public class ControlChart extends TaskDef {
	private static final Logger log = Logger.getLogger(FeaturizeDataHMM.class
			.getName());
	
	private void ControlChart() throws Exception {
		Var expRootPath = var("/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/changepoints");
		String frequency = "30";
		String day = "2";
		
		String clusterWorkspace = expRootPath + "/cluster";
		boolean useCluster = true;
		int clusterJobNum = 100;
		
		String kpre = "300";
		Array subjectIDs = array(Arrays.asList("1", "2", "3", "4", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "19", "20", "21", "22", "23", "24", "25"));
		
		Var dataPath = var("/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed");
		Var truncFileNames = dataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_truncated_day").cat(day).cat(".csv");
		Var dupFileNames = dataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_duplicates_day").cat(day).cat(".csv");
		Var seFileNames = dataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_start_and_end.csv");	

		Var ScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/control.chart.R");
		Var CallingPath = expRootPath.fileSep().cat(subjectIDs).cat("_scores.R");
		Var savePath = expRootPath.fileSep().cat(subjectIDs).cat("_scores.csv");
		
		ExecutorBuilder cc = rScript(ScriptPath, CallingPath, var("controlChart"),
				execConfig().setParallelizable(useCluster).setOnCluster(true)
				.setNumJobs(clusterJobNum).setClusterWorkspace(clusterWorkspace));

		cc.addParam("truncFile", String.class, truncFileNames,
				VerificationType.Before);
		cc.addParam("dupFile", String.class, dupFileNames,
				VerificationType.Before);
		cc.addParam("seFile", String.class, seFileNames,
				VerificationType.Before);
		cc.addParam("kpre", Integer.class, kpre);
		cc.addParam("savePath", String.class, savePath,
				VerificationType.After);

		cc.testMode();
		cc.execute();
	}
		
	public static void main(String[] args) {
		try {
			new ControlChart().ControlChart();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}