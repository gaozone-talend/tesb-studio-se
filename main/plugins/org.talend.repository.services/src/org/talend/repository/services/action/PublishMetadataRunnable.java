// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.services.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.xsd.XSDSchema;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.data.list.UniqueStringGenerator;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MappingTypeRetriever;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.builder.ConvertionHelper;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.XMLFileNode;
import org.talend.core.model.metadata.builder.connection.XmlFileConnection;
import org.talend.core.model.properties.ByteArray;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.XmlFileConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.update.RepositoryUpdateManager;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.PackageHelper;
import org.talend.datatools.xml.utils.ATreeNode;
import org.talend.datatools.xml.utils.OdaException;
import org.talend.datatools.xml.utils.XSDPopulationUtil2;
import org.talend.designer.core.DesignerPlugin;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.services.Activator;
import org.talend.repository.services.Messages;
import org.talend.repository.services.ui.RewriteSchemaDialog;
import org.talend.repository.services.ui.preferences.EsbSoapServicePreferencePage;
import org.talend.repository.services.utils.FolderNameUtil;
import org.talend.repository.services.utils.SchemaUtil;
import org.talend.repository.services.utils.WSDLUtils;

import orgomg.cwm.resource.record.RecordFactory;
import orgomg.cwm.resource.record.RecordFile;

public class PublishMetadataRunnable implements IRunnableWithProgress {

    private final Definition wsdlDefinition;

    private final Shell shell;

    private XSDPopulationUtil2 populationUtil;

    public PublishMetadataRunnable(Definition wsdlDefinition, Shell shell) {
        this.wsdlDefinition = wsdlDefinition;
        this.shell = shell;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        final IWorkspaceRunnable op = new IWorkspaceRunnable() {

            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                monitor.beginTask(Messages.PublishMetadataAction_Importing, 3);

                final Collection<XmlFileConnectionItem> xmlObjs;
                try {
                    xmlObjs = initFileConnection();
                } catch (Exception e) {
                    String message = (null != e.getMessage()) ? e.getMessage() : e.getClass().getName();
                    throw new CoreException(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(),
                            "Can't retrieve schemas from metadata: " + message, e));
                }
                Collection<XmlFileConnectionItem> selectTables;
                if (xmlObjs.size() > 0) {
                    RewriteSchemaDialogRunnable runnable = new RewriteSchemaDialogRunnable(shell, xmlObjs);
                    Display.getDefault().syncExec(runnable);
                    selectTables = runnable.getSelectTables();
                    if (null == selectTables) {
                        return;
                    }
                } else {
                    selectTables = Collections.emptyList();
                }
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }

                boolean validateWsdl = Activator.getDefault().getPreferenceStore()
                        .getBoolean(EsbSoapServicePreferencePage.ENABLE_WSDL_VALIDATION);
                if (validateWsdl) {
                    WSDLUtils.validateWsdl(wsdlDefinition.getDocumentBaseURI());
                }
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }

                try {
                    process(wsdlDefinition, selectTables);
                } catch (IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error during schema processing", e));
                }
                monitor.done();

            }

        };

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            ISchedulingRule schedulingRule = workspace.getRoot();
            // the update the project files need to be done in the workspace runnable to avoid all
            // notification
            // of changes before the end of the modifications.
            workspace.run(op, schedulingRule, IWorkspace.AVOID_UPDATE, monitor);
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        }

    }

    private static class RewriteSchemaDialogRunnable implements Runnable {

        private final Shell shell;

        private final Collection<XmlFileConnectionItem> xmlObjs;

        private Collection<XmlFileConnectionItem> selectTables;

        public RewriteSchemaDialogRunnable(Shell shell, Collection<XmlFileConnectionItem> xmlObjs) {
            this.shell = shell;
            this.xmlObjs = xmlObjs;
        }

        @Override
        public void run() {
            RewriteSchemaDialog selectContextDialog = new RewriteSchemaDialog(shell, xmlObjs);
            if (selectContextDialog.open() == Window.OK) {
                selectTables = selectContextDialog.getSelectionTables();
            } else {
                return;
            }
        }

        public Collection<XmlFileConnectionItem> getSelectTables() {
            return selectTables;
        }

    }

    private Collection<XmlFileConnectionItem> initFileConnection() throws URISyntaxException, PersistenceException {
        Collection<String> paths = getAllPaths();
        Collection<XmlFileConnectionItem> connItems = new ArrayList<XmlFileConnectionItem>();

        for (ConnectionItem item : DesignerPlugin.getDefault().getProxyRepositoryFactory().getMetadataConnectionsItem()) {
            if (item instanceof XmlFileConnectionItem && paths.contains(item.getState().getPath())
                    && !ConnectionHelper.getTables(item.getConnection()).isEmpty()) {
                connItems.add((XmlFileConnectionItem) item);
            }
        }
        return connItems;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getAllPaths() throws URISyntaxException {
        final Set<String> paths = new HashSet<String>();
        final Set<QName> portTypes = new HashSet<QName>();
        final Set<QName> alreadyCreated = new HashSet<QName>();
        for (Binding binding : (Collection<Binding>) wsdlDefinition.getAllBindings().values()) {
            final QName portType = binding.getPortType().getQName();
            if (portTypes.add(portType)) {
                for (BindingOperation operation : (Collection<BindingOperation>) binding.getBindingOperations()) {
                    Operation oper = operation.getOperation();
                    Input inDef = oper.getInput();
                    if (inDef != null) {
                        Message inMsg = inDef.getMessage();
                        addParamsToPath(portType, oper, inMsg, paths, alreadyCreated);
                    }

                    Output outDef = oper.getOutput();
                    if (outDef != null) {
                        Message outMsg = outDef.getMessage();
                        addParamsToPath(portType, oper, outMsg, paths, alreadyCreated);
                    }
                    for (Fault fault : (Collection<Fault>) oper.getFaults().values()) {
                        Message faultMsg = fault.getMessage();
                        addParamsToPath(portType, oper, faultMsg, paths, alreadyCreated);
                    }
                }
            }
        }
        return paths;
    }

    private static void addParamsToPath(final QName portType, Operation oper, Message msg, final Set<String> paths,
            final Set<QName> alreadyCreated) throws URISyntaxException {
        if (msg != null) {
            QName parameterFromMessage = getParameterFromMessage(msg);
            if (parameterFromMessage == null) {
                return;
            }
            if (alreadyCreated.add(parameterFromMessage)) {
                String folderPath = FolderNameUtil.getImportedXmlSchemaPath(parameterFromMessage.getNamespaceURI(),
                        portType.getLocalPart(), oper.getName());
                paths.add(folderPath);
            }
        }
    }

    private static QName getParameterFromMessage(Message msg) {
        // add first parameter from message.
        @SuppressWarnings("unchecked")
        Collection<Part> values = msg.getParts().values();
        if (values == null || values.isEmpty()) {
            return null;
        }
        Part part = values.iterator().next();
        if (part.getElementName() != null) {
            return part.getElementName();
        } else if (part.getTypeName() != null) {
            return part.getTypeName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void process(Definition wsdlDefinition, Collection<XmlFileConnectionItem> selectTables) throws IOException {
        Map<String, File> fileToSchemaMap = new HashMap<String, File>();
        File zip = null;
        final SchemaUtil schemaUtil = new SchemaUtil(wsdlDefinition);

        try {
            populationUtil = new XSDPopulationUtil2();
            for (XmlSchema schema : schemaUtil.getSchemas()) {
                File file = initFileContent(schema);
                String ns = schema.getTargetNamespace();
                fileToSchemaMap.put(ns != null ? ns : "", file);
                populationUtil.addSchema(file.getPath());
            }

            zip = File.createTempFile("tempXSDFile", ".zip");
            Collection<File> files = fileToSchemaMap.values();
            org.talend.utils.io.FilesUtils.zips(files.toArray(new File[files.size()]), zip.getPath());

            final Set<QName> portTypes = new HashSet<QName>();
            final Set<QName> alreadyCreated = new HashSet<QName>();
            for (Binding binding : (Collection<Binding>) wsdlDefinition.getAllBindings().values()) {
                final QName portType = binding.getPortType().getQName();
                if (portTypes.add(portType)) {
                    for (BindingOperation operation : (Collection<BindingOperation>) binding.getBindingOperations()) {
                        Operation oper = operation.getOperation();
                        Input inDef = oper.getInput();
                        if (inDef != null) {
                            Message inMsg = inDef.getMessage();
                            if (inMsg != null) {
                                // fix for TDI-20699
                                QName parameterFromMessage = getParameterFromMessage(inMsg);
                                if (parameterFromMessage == null) {
                                    continue;
                                }
                                if (alreadyCreated.add(parameterFromMessage)) {
                                    File schemaFile = fileToSchemaMap.get(parameterFromMessage.getNamespaceURI());
                                    if (null != schemaFile) {
                                        populateMessage2(parameterFromMessage, portType.getLocalPart(), oper.getName(),
                                                schemaFile, selectTables, zip);
                                    }
                                }
                            }
                        }

                        Output outDef = oper.getOutput();
                        if (outDef != null) {
                            Message outMsg = outDef.getMessage();
                            if (outMsg != null) {
                                QName parameterFromMessage = getParameterFromMessage(outMsg);
                                if (parameterFromMessage == null) {
                                    continue;
                                }
                                if (alreadyCreated.add(parameterFromMessage)) {
                                    File schemaFile = fileToSchemaMap.get(parameterFromMessage.getNamespaceURI());
                                    if (null != schemaFile) {
                                        populateMessage2(parameterFromMessage, portType.getLocalPart(), oper.getName(),
                                                schemaFile, selectTables, zip);
                                    }
                                }
                            }
                        }
                        for (Fault fault : (Collection<Fault>) oper.getFaults().values()) {
                            Message faultMsg = fault.getMessage();
                            if (faultMsg != null) {
                                QName parameterFromMessage = getParameterFromMessage(faultMsg);
                                if (parameterFromMessage == null) {
                                    continue;
                                }
                                if (alreadyCreated.add(parameterFromMessage)) {
                                    File schemaFile = fileToSchemaMap.get(parameterFromMessage.getNamespaceURI());
                                    if (null != schemaFile) {
                                        populateMessage2(parameterFromMessage, portType.getLocalPart(), oper.getName(),
                                                schemaFile, selectTables, zip);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            for (File file : fileToSchemaMap.values()) {
                file.delete();
            }
            if (null != zip) {
                zip.delete();
            }
        }
    }

    private int orderId;

    private boolean loopElementFound;

    /**
     * To optimize, right now it will write the xsd file many times. Since there is no clues if the parameters comes
     * from the same xsd, generate it everytime right now.
     * 
     * @param operationName
     * @param hashMap
     * @throws IOException
     */
    private void populateMessage2(QName parameter, String portTypeName, String operationName, File schemaFile,
            Collection<XmlFileConnectionItem> selectItems, File zip) throws IOException {
        String name = /* componentName + "_"+ */parameter.getLocalPart();
        XmlFileConnection connection = null;
        Property connectionProperty = null;
        XmlFileConnectionItem connectionItem = null;
        String oldConnectionId = null;
        String oldTableId = null;
        IMetadataTable oldMetadataTable = null;
        Map<String, String> oldTableMap = null;

        if (!selectItems.isEmpty()) {
            boolean needRewrite = false;
            for (XmlFileConnectionItem item : selectItems) {
                connectionProperty = item.getProperty();
                if (connectionProperty.getLabel().equals(name)) {
                    oldConnectionId = connectionProperty.getId();
                    connectionItem = item;
                    connection = (XmlFileConnection) connectionItem.getConnection();
                    needRewrite = true;
                    Set<MetadataTable> tables = ConnectionHelper.getTables(connection);
                    MetadataTable oldTable = null;
                    if (tables.size() > 0) {
                        oldTable = tables.toArray(new MetadataTable[0])[0];
                        oldTableId = oldTable.getId();
                        oldMetadataTable = ConvertionHelper.convert(oldTable);
                    }
                    oldTableMap = RepositoryUpdateManager.getOldTableIdAndNameMap(connectionItem, oldTable, false);
                    break;
                }
            }
            if (!needRewrite && !WSDLUtils.isNameValidInXmlFileConnection(parameter, portTypeName, operationName)) {
                return;
            }
        }

        // if (!exist) {
        connection = ConnectionFactory.eINSTANCE.createXmlFileConnection();
        connection.setName(ERepositoryObjectType.METADATA_FILE_XML.getKey());
        connectionItem = PropertiesFactory.eINSTANCE.createXmlFileConnectionItem();
        connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
        connectionProperty.setAuthor(((RepositoryContext) CoreRuntimePlugin.getInstance().getContext()
                .getProperty(Context.REPOSITORY_CONTEXT_KEY)).getUser());
        connectionProperty.setLabel(name);
        connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
        connectionProperty.setStatusCode(""); //$NON-NLS-1$

        connectionItem.setProperty(connectionProperty);
        connectionItem.setConnection(connection);

        connection.setInputModel(false);
        // }

        ByteArray byteArray = PropertiesFactory.eINSTANCE.createByteArray();
        byteArray.setInnerContentFromFile(zip);
        connection.setFileContent(byteArray.getInnerContent());

        // don't put any XSD directly inside the xml connection but put zip file
        // Use xsd schema file name + zip file name as xml file path in case we need get the root schema of xml
        // connection after.
        String schemaFileName = schemaFile.getName();
        schemaFileName = schemaFileName.substring(0, schemaFileName.lastIndexOf(".")); //$NON-NLS-1$
        connection.setXmlFilePath(schemaFileName.concat("_").concat(zip.getName())); //$NON-NLS-1$

        try {
            String filePath = schemaFile.getPath(); // name of xsd file needed
            XSDSchema xsdSchema = populationUtil.getXSDSchema(filePath);
            List<ATreeNode> rootNodes = populationUtil.getAllRootNodes(xsdSchema);

            ATreeNode node = null;

            // try to find the root element needed from XSD file.
            // note: if there is any prefix, it will get the node with the first correct name, no matter the prefix.

            // once the we can get the correct prefix value from the wsdl, this code should be modified.
            for (ATreeNode curNode : rootNodes) {
                String curName = (String) curNode.getValue();
                if (curName.contains(":")) { //$NON-NLS-1$
                    // if with prefix, don't care about it for now, just compare the name.
                    if (curName.split(":")[1].equals(name)) { //$NON-NLS-1$
                        node = curNode;
                        break;
                    }
                } else if (curName.equals(name)) {
                    node = curNode;
                    break;
                }
            }

            node = populationUtil.getSchemaTree(xsdSchema, node);
            orderId = 1;
            loopElementFound = false;
            if (ConnectionHelper.getTables(connection).isEmpty()) {
                MetadataTable table = ConnectionFactory.eINSTANCE.createMetadataTable();
                if (oldTableId != null) {
                    table.setId(oldTableId);
                } else {
                    table.setId(ProxyRepositoryFactory.getInstance().getNextId());
                }
                RecordFile record = (RecordFile) ConnectionHelper.getPackage(connection.getName(), connection, RecordFile.class);
                if (record != null) { // hywang
                    PackageHelper.addMetadataTable(table, record);
                } else {
                    RecordFile newrecord = RecordFactory.eINSTANCE.createRecordFile();
                    newrecord.setName(connection.getName());
                    ConnectionHelper.addPackage(newrecord, connection);
                    PackageHelper.addMetadataTable(table, newrecord);
                }
            }
            boolean haveElement = false;
            for (Object curNode : node.getChildren()) {
                if (((ATreeNode) curNode).getType() == ATreeNode.ELEMENT_TYPE) {
                    haveElement = true;
                    break;
                }
            }
            fillRootInfo(connection, node, "", !haveElement); //$NON-NLS-1$
        } catch (IOException e) {
            throw e;
        } catch (URISyntaxException e1) {
            ExceptionHandler.process(e1);
        } catch (OdaException e) {
            ExceptionHandler.process(e);
        }

        // save
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        connectionProperty.setId(factory.getNextId());
        try {
            // http://jira.talendforge.org/browse/TESB-3655 Remove possible
            // schema prefix
            String folderPath = FolderNameUtil.getImportedXmlSchemaPath(parameter.getNamespaceURI(), portTypeName, operationName);
            IPath path = new Path(folderPath);
            factory.create(connectionItem, path, true); // consider this as migration will overwrite the old metadata if
                                                        // existing in the same path
            if (oldConnectionId != null) {
                connectionItem.getProperty().setId(oldConnectionId);
                factory.save(connectionItem);
            }

            propagateSchemaChange(oldMetadataTable, oldTableMap, connection, connectionItem);

            ProxyRepositoryFactory.getInstance().saveProject(ProjectManager.getInstance().getCurrentProject());
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        } catch (URISyntaxException e) {
            ExceptionHandler.process(e);
        }
    }

    private void propagateSchemaChange(final IMetadataTable oldMetaTable, final Map<String, String> oldTableMap,
            final XmlFileConnection connection, final XmlFileConnectionItem connectionItem) {
        if (oldMetaTable == null) {
            return;
        }

        Display.getDefault().syncExec(()->{
            MetadataTable newTable = ConnectionHelper.getTables(connection).toArray(new MetadataTable[0])[0];
            RepositoryUpdateManager.updateSingleSchema(connectionItem, newTable, oldMetaTable, oldTableMap);
        });
    }

    private void fillRootInfo(XmlFileConnection connection, ATreeNode node, String path, boolean inLoop) {
        XMLFileNode xmlNode = ConnectionFactory.eINSTANCE.createXMLFileNode();
        xmlNode.setXMLPath(path + '/' + node.getValue());
        xmlNode.setOrder(orderId);
        orderId++;
        MappingTypeRetriever retriever;
        String nameWithoutPrefixForColumn;
        String curName = (String) node.getValue();
        if (curName.contains(":")) { //$NON-NLS-1$
            nameWithoutPrefixForColumn = curName.split(":")[1]; //$NON-NLS-1$
        } else {
            nameWithoutPrefixForColumn = curName;
        }
        retriever = MetadataTalendType.getMappingTypeRetriever("xsd_id"); //$NON-NLS-1$
        xmlNode.setAttribute("attri"); //$NON-NLS-1$
        xmlNode.setType(retriever.getDefaultSelectedTalendType(node.getDataType()));
        MetadataColumn column = null;
        MetadataTable metadataTable = ConnectionHelper.getTables(connection).toArray(new MetadataTable[0])[0];
        switch (node.getType()) {
        case ATreeNode.ATTRIBUTE_TYPE:
            // fix for TDI-20390 and TDI-20671 ,XMLPath for attribute should only store attribute name but not full
            // xpath
            xmlNode.setXMLPath("" + node.getValue()); //$NON-NLS-1$
            column = ConnectionFactory.eINSTANCE.createMetadataColumn();
            column.setTalendType(xmlNode.getType());
            String uniqueName = extractColumnName(nameWithoutPrefixForColumn, metadataTable.getColumns());
            column.setLabel(uniqueName);
            xmlNode.setRelatedColumn(uniqueName);
            metadataTable.getColumns().add(column);
            break;
        case ATreeNode.ELEMENT_TYPE:
            boolean haveElementOrAttributes = false;
            for (Object curNode : node.getChildren()) {
                if (((ATreeNode) curNode).getType() != ATreeNode.NAMESPACE_TYPE) {
                    haveElementOrAttributes = true;
                    break;
                }
            }
            if (!haveElementOrAttributes) {
                xmlNode.setAttribute("branch"); //$NON-NLS-1$
                column = ConnectionFactory.eINSTANCE.createMetadataColumn();
                column.setTalendType(xmlNode.getType());
                uniqueName = extractColumnName(nameWithoutPrefixForColumn, metadataTable.getColumns());
                column.setLabel(uniqueName);
                xmlNode.setRelatedColumn(uniqueName);
                metadataTable.getColumns().add(column);
            } else {
                xmlNode.setAttribute("main"); //$NON-NLS-1$
            }
            break;
        case ATreeNode.NAMESPACE_TYPE:
            xmlNode.setAttribute("ns"); //$NON-NLS-1$
            // specific for namespace... no path set, there is only the prefix value.
            // this value is saved now in node.getDataType()
            xmlNode.setXMLPath(node.getDataType());

            xmlNode.setDefaultValue((String) node.getValue());
            break;
        case ATreeNode.OTHER_TYPE:
            break;
        }
        boolean subElementsInLoop = inLoop;
        // will try to get the first element (branch or main), and set it as loop.
        if ((!loopElementFound && path.split("/").length == 2 && node.getType() == ATreeNode.ELEMENT_TYPE) || subElementsInLoop) { //$NON-NLS-1$
            connection.getLoop().add(xmlNode);

            loopElementFound = true;
            subElementsInLoop = true;
        } else {
            connection.getRoot().add(xmlNode);
        }
        if (node.getChildren().length > 0) {
            for (Object curNode : node.getChildren()) {
                fillRootInfo(connection, (ATreeNode) curNode, path + '/' + node.getValue(), subElementsInLoop);
            }
        }
    }

    private static File initFileContent(final XmlSchema schema) throws IOException {
        FileOutputStream outStream = null;
        try {
            File temfile = File.createTempFile("tempXSDFile", ".xsd"); //$NON-NLS-1$ //$NON-NLS-2$
            outStream = new FileOutputStream(temfile);
            schema.write(outStream); // this method hangs when using invalid wsdl.
            return temfile;
        } finally {
            outStream.close();
        }
    }

    private static final Pattern PATTERN_TOREPLACE = Pattern.compile("[^a-zA-Z0-9]"); //$NON-NLS-1$

    private static String extractColumnName(String currentExpr, List<MetadataColumn> fullSchemaTargetList) {

        String columnName = currentExpr.startsWith("@") ? currentExpr.substring(1) : currentExpr; //$NON-NLS-1$
        columnName = PATTERN_TOREPLACE.matcher(columnName).replaceAll("_"); //$NON-NLS-1$

        UniqueStringGenerator<MetadataColumn> uniqueStringGenerator = new UniqueStringGenerator<MetadataColumn>(columnName,
                fullSchemaTargetList) {

            @Override
            protected String getBeanString(MetadataColumn bean) {
                return bean.getLabel();
            }
        };
        columnName = uniqueStringGenerator.getUniqueString();
        return columnName;
    }

}
