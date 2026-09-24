export interface AuthResponse {
  token: string;
  email: string;
  nome: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  nome: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Session {
  id: string;
  status: 'IN_PROGRESS' | 'COMPLETED';
  createdAt: string;
  updatedAt: string;
  currentStepName: string;
}

export interface UploadResponse {
  sessionId: string;
  currentStep: string;
  currentStepIndex: number;
}

export type StepType = 'MANUAL_ENTRY' | 'DOCUMENT_UPLOAD' | 'AUTOMATIC';
export type ComparisonResult = 'OK' | 'MISMATCH' | 'PENDING';

export interface StepInfo {
  stepIndex: number;
  stepName: string;
  stepType: StepType;
  documentRequired: string | null;
  description: string;
  alreadyUploaded: boolean;
  value730: string | null;
  valueDocument: string | null;
  comparison: ComparisonResult;
  previewValue: string | null;
  isCompleted: boolean;
}

export interface UploadDocumentResponse {
  extractedValue: string;
  preview: string;
}

export interface ManualValueResponse {
  value730: string | null;
  valueUser: string;
  comparison: ComparisonResult;
}

export interface ComparisonResponse {
  value730: string | null;
  valueDocument: string;
  comparison: ComparisonResult;
}

export interface ConfirmStepRequest {
  confirmedValue: string;
}

export interface ConfirmStepResponse {
  nextStep: number | null;
  isCompleted: boolean;
}

export interface SubmitResponse {
  downloadUrl: string;
}

export interface Document {
  docType: string;
  uploadedAt: string;
  sessionId: string;
}

export interface CompletedStep {
  stepIndex: number;
  stepName: string;
  confirmedValue: string;
}
