package me.botsko.prism.commands;

import java.util.ArrayList;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.ApplierResult;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.appliers.Previewable;
import me.botsko.prism.appliers.Restore;
import me.botsko.prism.appliers.Rollback;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;

public class PreviewCommand implements SubHandler {
	
	/**
	 * 
	 */
	private Prism plugin;
	
	
	/**
	 * 
	 * @param plugin
	 * @return 
	 */
	public PreviewCommand(Prism plugin) {
		this.plugin = plugin;
	}
	
	
	/**
	 * Handle the command
	 */
	public void handle(CallInfo call) {
		if( call.getArgs().length >= 2 ){
			
			
			/**
			 * Apply previous preview changes
			 */
			if(call.getArg(1).equalsIgnoreCase("apply") ){
				if(plugin.playerActivePreviews.containsKey(call.getPlayer().getName())){
					PreviewSession previewSession = plugin.playerActivePreviews.get( call.getPlayer().getName() );
					previewSession.getPreviewer().apply_preview();
				}
				return;
			}
			
				
			/**
			 * Cancel preview
			 */
			if(call.getArg(1).equalsIgnoreCase("cancel") ){
				if(plugin.playerActivePreviews.containsKey(call.getPlayer().getName())){
					PreviewSession previewSession = plugin.playerActivePreviews.get( call.getPlayer().getName() );
					previewSession.getPreviewer().cancel_preview();
				}
				return;
			}
			
			
			// Ensure no current preview is waiting
			if(plugin.playerActivePreviews.containsKey(call.getPlayer().getName())){
				call.getPlayer().sendMessage( plugin.playerError("You have an existing preview pending. Please apply or cancel before moving on.") );
				return;
			}
			
			
			/**
			 * Begin a rollback or restore preview
			 */
			if( call.getArg(1).equalsIgnoreCase("rollback") || call.getArg(1).equalsIgnoreCase("restore") ){
				
				QueryParameters parameters = PreprocessArgs.process( plugin, call.getPlayer(), call.getArgs(), "rollback", 2 );
				if(parameters == null){
					return;
				}
			
				// Perform preview
				ActionsQuery aq = new ActionsQuery(plugin);
				QueryResult results = aq.lookup( call.getPlayer(), parameters );
				if(!results.getActionResults().isEmpty()){
					
					call.getPlayer().sendMessage( plugin.playerHeaderMsg("Beginning rollback preview...") );
					
					ApplierResult result = null;
					Previewable rs = null;
					if(call.getArg(1).equalsIgnoreCase("rollback")){
						rs = new Rollback( plugin, call.getPlayer(), results.getActionResults(), parameters );
						result = rs.preview();
					}
					if(call.getArg(1).equalsIgnoreCase("restore")){
						rs = new Restore( plugin, call.getPlayer(), results.getActionResults(), parameters );
						result = rs.preview();
					}
					if(result != null){
					
						// If we're in a preview and changes would be applied...
						if(result.isPreview() && result.getChanges_applied() > 0){
							// Append the preview and blocks temporarily
							PreviewSession ps = new PreviewSession( call.getPlayer(), rs, result );
							plugin.playerActivePreviews.put(call.getPlayer().getName(), ps);
						}
						
						// Send any messages to user
						ArrayList<String> responses = result.getMessages();
						if(!responses.isEmpty()){
							for(String resp : responses){
								call.getPlayer().sendMessage(resp);
							}
						}
					}
				} else {
					// @todo no results
				}
			}
		}
	}
}