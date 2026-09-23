async function loadDashboard() {
    const response = await fetch("/api/dashboard");
    const data = await response.json();

    renderStats(data.overview);
    renderModuleCards("public-modules", data.publicModules);
    renderModuleCards("teacher-modules", data.teacherModules);
    renderModuleCards("student-modules", data.studentModules);
    renderAnnouncements(data.announcements);
    renderResources(data.resources);
    renderTasks(data.tasks);
    renderSubmissions(data.submissions);
    renderAiSuggestions(data.aiSuggestions);
}

function renderStats(overview) {
    const stats = [
        { label: "平台用户数", value: overview.userCount },
        { label: "教学资源数", value: overview.resourceCount },
        { label: "进行中任务", value: overview.activeTaskCount },
        { label: "多模态记录数", value: overview.multimodalRecordCount }
    ];

    document.getElementById("overview").innerHTML = stats.map((item) => `
        <article class="stat-card">
            <p class="label">${item.label}</p>
            <p class="value">${item.value}</p>
        </article>
    `).join("");
}

function renderModuleCards(targetId, modules) {
    document.getElementById(targetId).innerHTML = modules.map((item) => `
        <article class="module-card">
            <h4>${item.title}</h4>
            <p>${item.description}</p>
            <ul>${item.highlights.map((highlight) => `<li>${highlight}</li>`).join("")}</ul>
        </article>
    `).join("");
}

function renderAnnouncements(items) {
    document.getElementById("announcements").innerHTML = items.map((item) => `
        <article class="list-item">
            <strong>${item.title}</strong>
            <p>${item.summary}</p>
            <div class="meta">${item.category} | ${item.publishTime}</div>
        </article>
    `).join("");
}

function renderResources(items) {
    document.getElementById("resources").innerHTML = items.map((item) => `
        <article class="list-item">
            <strong>${item.title}</strong>
            <p>${item.type} · 面向 ${item.audience}</p>
            <div class="meta">${item.action}</div>
        </article>
    `).join("");
}

function renderTasks(items) {
    document.getElementById("tasks").innerHTML = items.map((item) => `
        <article class="list-item">
            <strong>${item.title}</strong>
            <p>难度：${item.difficulty}，提交方式：${item.submissionMode}</p>
            <div class="meta">发布教师：${item.owner} | 截止：${item.deadline}</div>
        </article>
    `).join("");
}

function renderSubmissions(items) {
    const header = `
        <div class="table-row table-head">
            <div>学生</div>
            <div>任务</div>
            <div>形式</div>
            <div>教师批注</div>
            <div>成绩</div>
        </div>
    `;

    const rows = items.map((item) => `
        <div class="table-row">
            <div><strong>${item.studentName}</strong></div>
            <div>${item.taskTitle}</div>
            <div>${item.mediaType}</div>
            <div><p>${item.teacherComment}</p></div>
            <div><strong>${item.score}</strong></div>
        </div>
    `).join("");

    document.getElementById("submissions").innerHTML = header + rows;
}

function renderAiSuggestions(items) {
    document.getElementById("ai-suggestions").innerHTML = items.map((item) => `
        <article class="ai-card">
            <strong>${item.title}</strong>
            <p>${item.output}</p>
            <div class="meta">${item.scenario} · ${item.role === "teacher" ? "教师视角" : "学生视角"}</div>
        </article>
    `).join("");
}

loadDashboard().catch(() => {
    document.body.insertAdjacentHTML(
        "beforeend",
        "<p style='padding:16px;color:#7b2f17;'>数据加载失败，请检查后端服务是否已启动。</p>"
    );
});
