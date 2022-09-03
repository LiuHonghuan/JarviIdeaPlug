import com.google.common.base.Strings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import groovy.lang.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author honghuan.Liu
 * @date 2022/9/3 21:28
 */
public class JarvisIdeaAction extends AnAction {

    static Pattern PATTERN_MAPPER_NAMESPACE = Pattern.compile("<mapper\\s+namespace=\"([\\.\\w]+)\".*>");

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String selectedText = "";
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        try {
            selectedText = editor.getSelectionModel().getSelectedText();
        } catch (Exception exception) {
        }

        if (!selectedText.contains(".")) {
            return;
        }
        String[] split = selectedText.split("\\.");
        if (split.length != 2) {
            return;
        }

        MyMapperConfig mapperConfig = new MyMapperConfig(split[0], split[1]);
        // 文件
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (Objects.isNull(virtualFile)) {
            return;
        }
        // 文件对应模块
        Module moduleForFile = ProjectRootManager.getInstance(project)
                .getFileIndex()
                .getModuleForFile(virtualFile);
        if (Objects.isNull(moduleForFile)) {
            return;
        }
        // 查询所有xml后缀的文件
        Collection<VirtualFile> vfiles = FilenameIndex.getAllFilesByExt(project, "xml", GlobalSearchScope.moduleScope(moduleForFile));
        PsiManager psiManager = PsiManager.getInstance(project);
        Tuple2<PsiFile, String> namespaceFile = getNamespaceFile(psiManager, vfiles, mapperConfig);


        if (Objects.isNull(namespaceFile)) {
            return;
        }
        PsiFile psiFile = namespaceFile.getFirst();
        String context = namespaceFile.getSecond();
        String keyword = mapperConfig.getKeyword();

        int offset = context.indexOf("\"" + keyword + "\"");
        if (offset == -1) {
            return;
        }

        // 打开文件, 并跳转到指定位置
        OpenFileDescriptor d = new OpenFileDescriptor(project, psiFile.getVirtualFile(), offset);
        d.navigate(true);

    }



    private Tuple2<PsiFile, String> getNamespaceFile(PsiManager psiManager, Collection<VirtualFile> vfiles, MyMapperConfig mapperConfig) {
        for (VirtualFile vfile : vfiles) {
            PsiFile psiFile = psiManager.findFile(vfile);
            if (Objects.equals(false, isMybatisFile(psiFile))) {
                continue;
            }

            String fileText = psiFile.getText();
            if (Strings.isNullOrEmpty(fileText)) {
                continue;
            }
            Matcher matcher = PATTERN_MAPPER_NAMESPACE.matcher(fileText);
            if (matcher.find()) {
                String namespace = matcher.group(1);
                if (namespace.toLowerCase().endsWith(mapperConfig.getNamespace().toLowerCase())) {
                    return new Tuple2(psiFile, fileText);
                }
            }
        }
        return null;
    }

    public static boolean isMybatisFile(@Nullable PsiFile file) {
        Boolean mybatisFile = null;
        if (file == null) {
            mybatisFile = false;
        }
        if (mybatisFile == null) {
            if (!isXmlFile(file)) {
                mybatisFile = false;
            }
        }
        if (mybatisFile == null) {
            XmlTag rootTag = ((XmlFile) file).getRootTag();
            if (rootTag == null) {
                mybatisFile = false;
            }
            if (mybatisFile == null) {
                if (!"mapper".equals(rootTag.getName())) {
                    mybatisFile = false;
                }
            }
        }
        if (mybatisFile == null) {
            mybatisFile = true;
        }
        return mybatisFile;
    }

    static boolean isXmlFile(@NotNull PsiFile file) {
        return file instanceof XmlFile;
    }
}
