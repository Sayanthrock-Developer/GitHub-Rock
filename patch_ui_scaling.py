files = [
    'app/src/main/java/com/sayanthrock/githubrock/ui/screens/RepositoryDetailScreen.kt',
    'app/src/main/java/com/sayanthrock/githubrock/ui/screens/RepositoryHubContent.kt',
    'app/src/main/java/com/sayanthrock/githubrock/ui/screens/RepositoryShowcaseScreen.kt'
]

old_scale_str = "contentScale = ContentScale.FillWidth"
new_scale_str = "contentScale = ContentScale.Inside"

for file_path in files:
    with open(file_path, 'r') as f:
        content = f.read()

    if old_scale_str in content:
        content = content.replace(old_scale_str, new_scale_str)

    with open(file_path, 'w') as f:
        f.write(content)
