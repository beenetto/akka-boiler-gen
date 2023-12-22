import os
import shutil


def generate_akka_project(project_name, k8s_namespace, project_owner, project_description):
    """Generates an Akka project with the given name."""

    project_dir = os.path.join(os.getcwd(), f'output/{project_name.strip()}')
    shutil.copytree("templates",  project_dir)

    for root, dirs, files in os.walk(project_dir):
        for file in files:
            file_path = os.path.join(root, file)
            with open(file_path, "r") as in_file:
                file_contents = in_file.read()
                modified_contents = file_contents.replace(
                    "*project_name*", project_name.strip()).replace(
                    "*k8s_namespace*", k8s_namespace.strip()).replace(
                    "*project_owner*", project_owner.strip()).replace(
                    "*project_description*", project_description.strip()).replace(
                    "*module_name*", module_name.strip())
                with open(file_path, "w") as output_file:
                    output_file.write(modified_contents)


if __name__ == "__main__":
    project_name = input("Enter the name of the Akka project: ")
    module_name = input("Enter the name of the Akka module: ")
    k8s_namespace = input("Enter K8s : ")
    project_owner = input("Enter the project owner: ") or ""
    project_description = input("Enter the project description: ") or ""

    print(f"Project name: {project_name}")
    print(f"Module name: {module_name}")
    print(f"K8s namespace: {k8s_namespace}")
    print(f"Owner: {project_owner}")
    print(f"Description: {project_description}")

    confirmation = input(f"Proceed with creating project ${project_name}? (y/n): ")

    if confirmation == "y":
        generate_akka_project(project_name, k8s_namespace, project_owner, project_description)
        print(f"${project_name} created successfully.")
    else:
        print("Files not copied.")
