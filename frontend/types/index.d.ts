declare type SearchParamProps = {
  params: { [key: string]: string };
  searchParams: { [key: string]: string | string[] | undefined };
};

declare interface LabelProps {
    labels?: Label | Label[] | null;
}

declare interface CommitMessagesTabProps {
    messages?: Message | Message[] | null;
}

declare interface StageEnvironment {
  stageEnvVariables: Record<string, string>;
  results: unknown | null; 
}

declare interface Stage {
  name: string;
  image: string;
  repo: string | null;
  stageEnvironment: StageEnvironment;
  script: string;
}

declare interface PipelineParameter {
  name: string;
  defaultValue: string;
  description: string;
}

declare interface PipelineDefinition {
  id: string;
  name: string;
  authorEmail: string;
  parameters: PipelineParameter[];
  envVariables: Record<string, string>;
  stages: Stage[][];
}

declare interface PipelineRun {
    runId: string,
    pipelineId: string,
    status: string,
    startTime: string,
    endTime: string,
    runBy: string
}

declare interface KeyValueMap {
    [key: string]: string | number;
}

declare interface StageInfo {
    stageId: string;
    name: string;
    image: string;
    stageEnvVariables: KeyValueMap;
    resultEnvs: KeyValueMap;
    startTime: string | null;
    endTime: string | null;
    status: string;
    message: string;
}

declare interface PipelineRunDetails {
    runId: string;
    pipelineId: string;
    parameters: KeyValueMap;
    envVariables: KeyValueMap;
    stagesInfo: StageInfo[];
    labels: KeyValueMap;
    status: string;
    startTime: string;
    endTime: string;
    runBy: string;
}

interface FileTreeNode {
    blobHash: string | null;
    children: FileTreeNode[] | null;
    name: string;
    path: string;
    type: 'FILE' | 'DIRECTORY';
}

interface CommitInfoProps {
    commitInfo: Commit | null
}

interface PipelineInfoProps {
  pipelineInfo?: PipelineDefinition;
    handleRunPipeline: () => void;
}

interface PipelineRunsTableProps {
    pipelineRuns: PipelineRun[];
}

interface Pipeline {
  id: string;
  name: string;
  authorEmail: string;
  lastUpdated: string;
}


interface PipelineTableProps {
  filter?: string;
}

interface PipelineParametersFormProps {
  pipelineId: string;
  parameters: PipelineParameter[];
  onCancel: () => void;
}

interface Log {
    timestampNs: string,
    message: string
}

// ========================================

declare type SignUpParams = {
  firstName: string;
  lastName: string;
  address1: string;
  city: string;
  state: string;
  postalCode: string;
  dateOfBirth: string;
  ssn: string;
  email: string;
  password: string;
};

declare type LoginUser = {
  email: string;
  password: string;
};

declare type User = {
  $id: string;
  email: string;
  firstName: string;
  lastName: string;
  name: string;
  accessGroups?: string[];
};

declare type NewUserParams = {
  userId: string;
  email: string;
  name: string;
  password: string;
};

declare type Message = {
  text: string;
  authorEmail: string;
  commitId: string;
};

declare type Label = {
  name: string,
  value: number,
  authorEmail?: string,
  commitId?: string
}

declare type LabelDTO = {
  name: string;
  value: number;
  authorEmail?: string;
  commitId: string;
}


declare type Commit = {
  id: string;
  message: string;
  authorEmail: string;
  messages?: Message | Message[];
  labels?: Label | Label[];
  branchName: string;
  timestamp: number;
  status: string;
};

declare type CommitFile = {
  id: string;
  path: string;
  blobHash: string;
};

declare type Branch = {
  id: number;
  name: string;
  headCommitId: string;
};

declare type Category = "Food and Drink" | "Travel" | "Transfer";

declare type CategoryCount = {
  name: string;
  count: number;
  totalCount: number;
};


declare interface HeaderBoxProps {
  type?: "title" | "greeting";
  title: string;
  subtext: string;
  user?: string;
}

declare interface MobileNavProps {
  user: User;
}

declare interface PageHeaderProps {
  topTitle: string;
  bottomTitle: string;
  topDescription: string;
  bottomDescription: string;
  connectBank?: boolean;
}

declare interface PaginationProps {
  page: number;
  totalPages: number;
}

declare interface AuthFormProps {
  type: "sign-in" | "sign-up";
}

declare interface FooterProps {
  user: User;
  type?: "desktop" | "mobile";
}

declare interface RightSidebarProps {
  user: User;
  transactions: Transaction[];
  banks: Bank[] & Account[];
}

declare interface SiderbarProps {
  user: User;
}

declare interface CategoryBadgeProps {
  category: string;
}

declare interface DoughnutChartProps {
  accounts: Account[];
}

// Actions
declare interface getAccountsProps {
  userId: string;
}

declare interface getAccountProps {
  appwriteItemId: string;
}

declare interface signInProps {
  email: string;
  password: string;
}

declare interface getUserInfoProps {
  userId: string;
}