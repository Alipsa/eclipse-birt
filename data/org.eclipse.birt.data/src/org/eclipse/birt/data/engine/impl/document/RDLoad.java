/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.impl.document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultClass;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.ResultMetaData;
import org.eclipse.birt.data.engine.impl.document.viewing.DataSetResultSet;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprDataResultSet1;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprDataResultSet2;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprMetaInfo;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprMetaUtil;
import org.eclipse.birt.data.engine.impl.document.viewing.IExprDataResultSet;
import org.eclipse.birt.data.engine.odi.IResultClass;

/**
 * Load data from input stream.
 */
public class RDLoad
{
	private int version;
	
	//
	private StreamManager streamManager;
	private RDSubQueryUtil subQueryUtil;
	
	/**
	 * @param context
	 * @param queryResultID
	 * @param subQueryName
	 * @param currParentIndex
	 * @throws DataException
	 */
	RDLoad( DataEngineContext context, QueryResultInfo queryResultInfo )
			throws DataException
	{
		subQueryUtil = new RDSubQueryUtil( context,
				QueryResultIDUtil.getRealStreamID( queryResultInfo.getRootQueryResultID( ),
						queryResultInfo.getSelfQueryResultID( ) ),
				queryResultInfo.getSubQueryName( ) );
		streamManager = new StreamManager( context,
				new QueryResultInfo( queryResultInfo.getRootQueryResultID( ),
						queryResultInfo.getParentQueryResultID( ),
						queryResultInfo.getSelfQueryResultID( ),
						queryResultInfo.getSubQueryName( ),
						queryResultInfo.getSubQueryName( ) == null
								? 0
								: getSubQueryIndex( queryResultInfo.getIndex( ) ) ) );
		
		this.version = VersionManager.getVersion( context );
	}
	
	/**
	 * @param currParentIndex
	 * @return
	 * @throws DataException
	 */
	int getSubQueryIndex( int currParentIndex ) throws DataException
	{
		return subQueryUtil.getSubQueryIndex( currParentIndex );
	}
	
	/**
	 * @return result meta data
	 * @throws DataException
	 */
	ResultMetaData loadResultMetaData( ) throws DataException
	{
		return new ResultMetaData( loadResultClass( ) );
	}
	
	/**
	 * This is used for PRESENTATION, the data in report document as the
	 * CachedResultSet.
	 * 
	 * @return
	 * @throws DataException
	 */
	ExprResultSet loadExprResultSet( ) throws DataException
	{
		if ( streamManager.isSecondRD( ) == true
				&& streamManager.isSubquery( ) == true )
			return new ExprResultSet2( streamManager,
					version,
					streamManager.isSecondRD( ) );
		
		return new ExprResultSet( streamManager,
				loadGroupUtil( StreamManager.ROOT_STREAM, StreamManager.SELF_SCOPE ),
				version,
				streamManager.isSecondRD( ) );
	}
	
	/**
	 * @return
	 * @throws DataException
	 */
	public RDGroupUtil loadRootGroupUtil( ) throws DataException
	{
		return loadGroupUtil( StreamManager.ROOT_STREAM, StreamManager.BASE_SCOPE );
	}
	
	/**
	 * @return
	 * @throws DataException
	 */
	private RDGroupUtil loadGroupUtil( int streamPos, int streamScope ) throws DataException
	{
		InputStream stream = streamManager.getInStream( DataEngineContext.GROUP_INFO_STREAM,
				streamPos,
				streamScope );
		BufferedInputStream buffStream = new BufferedInputStream( stream );
		RDGroupUtil rdGroupUtil = new RDGroupUtil( buffStream );
		try
		{
			buffStream.close( );
			stream.close( );
		}
		catch ( IOException e )
		{
			// ignore it
		}

		return rdGroupUtil;
	}
	
	/**
	 * This is used for UPDATE, the data in report document as data source for
	 * transformation.
	 * 
	 * @return
	 * @throws DataException
	 */
	public IExprDataResultSet loadExprDataResultSet( ) throws DataException
	{
		if ( version == VersionManager.VERSION_2_0 )
			throw new DataException( ResourceConstants.WRONG_VERSION );

		InputStream inputStream = streamManager.getInStream( DataEngineContext.EXPR_META_STREAM,
				StreamManager.ROOT_STREAM, StreamManager.BASE_SCOPE );
		BufferedInputStream buffStream = new BufferedInputStream( inputStream );
		ExprMetaInfo[] exprMetas = ExprMetaUtil.loadExprMetaInfo( buffStream );
		try
		{
			buffStream.close( );
			inputStream.close( );
		}
		catch ( IOException e )
		{
			// ignore
		}

		// This is a special case, that the stream needs to be close at the code
		// of ExprDataResultSet
		IExprDataResultSet exprDataResultSet = null;
		if ( streamManager.isBasedOnSecondRD( ) == false )
			exprDataResultSet = new ExprDataResultSet1( streamManager.getInStream( DataEngineContext.EXPR_VALUE_STREAM,
					StreamManager.ROOT_STREAM,
					StreamManager.BASE_SCOPE ),
					streamManager.getInStream( DataEngineContext.EXPR_ROWLEN_STREAM,
							StreamManager.ROOT_STREAM,
							StreamManager.BASE_SCOPE ),
					exprMetas );
		else
			exprDataResultSet = new ExprDataResultSet2( streamManager.getInStream( DataEngineContext.EXPR_VALUE_STREAM,
					StreamManager.ROOT_STREAM,
					StreamManager.BASE_SCOPE ),
					streamManager.getInStream( DataEngineContext.EXPR_ROWLEN_STREAM,
							StreamManager.ROOT_STREAM,
							StreamManager.BASE_SCOPE ),
					streamManager.getInStream( DataEngineContext.ROW_INDEX_STREAM,
							StreamManager.ROOT_STREAM,
							StreamManager.PARENT_SCOPE ),
					exprMetas );

		return exprDataResultSet;
	}
	
	/**
	 * @return
	 * @throws DataException
	 */
	private IResultClass loadResultClass( ) throws DataException
	{
		InputStream stream = streamManager.getInStream( DataEngineContext.DATASET_META_STREAM,
				StreamManager.ROOT_STREAM, StreamManager.BASE_SCOPE );
		BufferedInputStream buffStream = new BufferedInputStream( stream );
		IResultClass resultClass = new ResultClass( buffStream );
		try
		{
			buffStream.close( );
			stream.close( );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.RD_LOAD_ERROR,
					e,
					"Result Class" );
		}

		return resultClass;
	}
	
	/**
	 * @return
	 * @throws DataException
	 */
	public DataSetResultSet loadDataSetData( ) throws DataException
	{
		InputStream stream = streamManager.getInStream( DataEngineContext.DATASET_DATA_STREAM,
				StreamManager.ROOT_STREAM, StreamManager.BASE_SCOPE );
		BufferedInputStream buffStream = new BufferedInputStream( stream );
		DataSetResultSet populator = new DataSetResultSet( buffStream, this.loadResultClass() );

		return populator;
	}
	
	public List loadFilterDefn( int streamPos, int streamScope ) throws DataException
	{
		InputStream inputStream = streamManager.getInStream( DataEngineContext.FILTER_DEFN_STREAM,
				streamPos,
				streamScope );
		List filterList = FilterDefnUtil.loadFilterDefn( inputStream );
		try
		{
			inputStream.close( );
		}
		catch ( IOException e )
		{
			// ignore
		}

		return filterList;
	}
		
	public List loadGroupDefn( int streamPos, int streamScope ) throws DataException
	{
		InputStream inputStream = streamManager.getInStream( DataEngineContext.GROUP_DEFN_STREAM,
				streamPos, streamScope );
		List groupList = GroupDefnUtil.loadGroupDefn( inputStream );
		try
		{
			inputStream.close( );
		}
		catch ( IOException e )
		{
			// ignore
		}

		return groupList;
	}
	
}
