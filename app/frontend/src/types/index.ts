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
  status: 'IN_PROGRESS' | 'COMPLETED' | 'SUBMITTED';
  createdAt: string;
  currentStepName: string;
}

export interface UploadResponse {
  sessionId: string;
  currentStep: number;
}

export interface StepInfo {
  stepIndex: number;
  stepName: string;
  documentRequired: boolean;
  documentDescription: string;
  alreadyUploaded: boolean;
  previewValue: string;
  isAutomatic?: boolean;
}

export interface UploadDocumentResponse {
  extractedValue: string;
  preview: string;
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
