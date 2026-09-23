package com.example.teachingplatform.dashboard.service;

import com.example.teachingplatform.dashboard.model.AiSuggestion;
import com.example.teachingplatform.dashboard.model.Announcement;
import com.example.teachingplatform.dashboard.model.DashboardData;
import com.example.teachingplatform.dashboard.model.LearningTask;
import com.example.teachingplatform.dashboard.model.ModuleCard;
import com.example.teachingplatform.dashboard.model.OverviewStats;
import com.example.teachingplatform.dashboard.model.ResourceItem;
import com.example.teachingplatform.dashboard.model.SubmissionRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    public DashboardData getDashboardData() {
        return new DashboardData(
                new OverviewStats(128, 46, 9, 312),
                buildPublicModules(),
                buildTeacherModules(),
                buildStudentModules(),
                buildAnnouncements(),
                buildResources(),
                buildTasks(),
                buildSubmissions(),
                buildAiSuggestions()
        );
    }

    private List<ModuleCard> buildPublicModules() {
        return List.of(
                new ModuleCard(
                        "用户管理",
                        "支持注册、登录与教师/学生身份区分，满足平台基础访问控制需求。",
                        List.of("账号注册与登录", "角色身份识别", "基础权限隔离")
                ),
                new ModuleCard(
                        "个人中心",
                        "统一管理个人资料、操作记录与收藏内容，形成学习过程入口。",
                        List.of("信息编辑", "操作记录", "收藏夹管理")
                ),
                new ModuleCard(
                        "系统公告",
                        "面向教学通知、任务提醒与平台更新的统一信息发布模块。",
                        List.of("教学通知", "任务提醒", "平台更新")
                )
        );
    }

    private List<ModuleCard> buildTeacherModules() {
        return List.of(
                new ModuleCard(
                        "教学资源管理",
                        "对菜谱课件、实操视频与素材文件进行上传、分类、编辑与删除。",
                        List.of("课件上传", "实操视频管理", "资源分类检索")
                ),
                new ModuleCard(
                        "学习任务管理",
                        "发布厨学实操任务，设置要求，进行批改与成绩录入。",
                        List.of("任务发布", "要求编辑", "成绩录入")
                ),
                new ModuleCard(
                        "多模态学习数据查看",
                        "集中查看学生提交的图片、视频与学习心得，支持批注。",
                        List.of("图片/视频查看", "文本心得归档", "过程批注")
                ),
                new ModuleCard(
                        "AI 智能辅助",
                        "对教学资料整理、任务说明生成和学生成果初步分析提供辅助。",
                        List.of("资料整理", "任务说明生成", "成果初步分析")
                ),
                new ModuleCard(
                        "学习成果管理",
                        "对学生优秀作品进行评选、展示与导出，支撑课程成果沉淀。",
                        List.of("优秀作品评选", "成果展示", "批量导出")
                ),
                new ModuleCard(
                        "教学数据统计",
                        "统计任务完成率、提交情况与学习参与度，形成可视化教学反馈。",
                        List.of("完成率统计", "提交情况汇总", "参与度可视化")
                )
        );
    }

    private List<ModuleCard> buildStudentModules() {
        return List.of(
                new ModuleCard(
                        "学习资源获取",
                        "查看与下载厨学课件、实操视频和菜谱素材，支持收藏。",
                        List.of("资源查看", "课件下载", "素材收藏")
                ),
                new ModuleCard(
                        "学习任务参与",
                        "接收任务、查看要求并在线提交学习成果，构成主要学习入口。",
                        List.of("任务接收", "要求查看", "在线提交")
                ),
                new ModuleCard(
                        "多模态实践记录",
                        "上传实操图片、视频和学习心得，沉淀实践过程档案。",
                        List.of("图片上传", "视频上传", "过程批注")
                ),
                new ModuleCard(
                        "AI 智能辅助",
                        "提供实操问题解答、菜谱做法指导与学习资源推荐。",
                        List.of("问题解答", "做法指导", "资源推荐")
                ),
                new ModuleCard(
                        "学习成果查看",
                        "查看教师批注、批改结果和成绩，浏览优秀成果案例。",
                        List.of("批改结果", "成绩查询", "优秀案例观摩")
                ),
                new ModuleCard(
                        "个人学习记录",
                        "沉淀任务完成记录、实践归档与个人学习数据汇总。",
                        List.of("任务记录", "过程归档", "学习数据汇总")
                )
        );
    }

    private List<Announcement> buildAnnouncements() {
        return List.of(
                new Announcement("刀工训练周安排发布", "教学通知", "2026-03-18 09:00", "本周统一安排蓑衣花刀与丝丁配比练习，请按班级时间参与。"),
                new Announcement("红烧类菜品任务提交提醒", "任务提醒", "2026-03-17 16:30", "三道红烧类作品需同步提交成品图、过程图和反思文本。"),
                new Announcement("平台新增多模态批注入口", "平台更新", "2026-03-16 10:15", "教师端已支持对图片和视频记录进行集中批注。")
        );
    }

    private List<ResourceItem> buildResources() {
        return List.of(
                new ResourceItem("宫保鸡丁标准化课件", "PPT 课件", "教师/学生", "查看与下载"),
                new ResourceItem("热菜实操分镜视频", "教学视频", "学生", "在线播放"),
                new ResourceItem("浙菜基础菜谱素材包", "图文素材", "教师", "上传与管理")
        );
    }

    private List<LearningTask> buildTasks() {
        return List.of(
                new LearningTask("宫保鸡丁火候控制实训", "中等", "2026-03-22 20:00", "张老师", "图片 + 文本"),
                new LearningTask("基础刀工视频记录任务", "基础", "2026-03-20 18:00", "李老师", "视频 + 批注"),
                new LearningTask("创新摆盘作品展示", "进阶", "2026-03-25 21:00", "王老师", "图片 + 心得")
        );
    }

    private List<SubmissionRecord> buildSubmissions() {
        return List.of(
                new SubmissionRecord("陈晨", "宫保鸡丁火候控制实训", "成品图 + 过程图", "翻锅动作稳定，建议优化芡汁浓度。", "92"),
                new SubmissionRecord("林悦", "基础刀工视频记录任务", "短视频", "切配节奏较好，建议加强粗细统一性。", "88"),
                new SubmissionRecord("周航", "创新摆盘作品展示", "图片 + 文本心得", "作品创意强，文本反思完整。", "95")
        );
    }

    private List<AiSuggestion> buildAiSuggestions() {
        return List.of(
                new AiSuggestion("任务说明生成", "教师端", "根据‘红烧排骨实训’自动整理任务目标、材料清单和评分提示。", "teacher"),
                new AiSuggestion("学习资源推荐", "学生端", "根据‘浙菜入门’主题推荐菜谱做法、火候技巧与参考视频。", "student"),
                new AiSuggestion("成果初步分析", "教师端", "对学生提交的图片与文字进行初步描述，生成课堂点评参考。", "teacher")
        );
    }
}
