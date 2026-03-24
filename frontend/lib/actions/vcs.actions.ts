import api from "../api";

export const getCommits = async (repoId: string) => {
  try {
    const response = await api.get(`/api/v1/vcs/repos/${repoId}/commits`);
    return response.data;
  } catch (error) {
    console.error("Error fetching commits:", error);
    return [];
  }
};

export const getBranches = async (repoId: string) => {
  try {
    const response = await api.get(`/api/v1/vcs/repos/${repoId}/branches`);
    return response.data;
  } catch (error) {
    console.error("Error fetching branches:", error);
    return [];
  }
};

export const getCommit = async (commitId: string) => {
  try {
    const response = await api.get(`/api/v1/vcs/commits/${commitId}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching commit:", error);
    return null;
  }
};
